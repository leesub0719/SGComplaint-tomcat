package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.JobApplicationItem;
import com.transit.SGComplaint.DTO.JobApplicationRequest;
import com.transit.SGComplaint.DTO.JobDocumentOption;
import com.transit.SGComplaint.DTO.JobPostingDetail;
import com.transit.SGComplaint.DTO.JobPostingItem;
import com.transit.SGComplaint.DTO.JobPostingRequest;
import com.transit.SGComplaint.domain.JobApplication;
import com.transit.SGComplaint.domain.JobApplicationFile;
import com.transit.SGComplaint.domain.JobPosting;
import com.transit.SGComplaint.mapper.RecruitmentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecruitmentService {
    private static final int PAGE_SIZE = 10;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 40L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf",
            "doc", "docx", "hwp", "hwpx", "xls", "xlsx", "csv");
    private static final List<JobDocumentOption> DOCUMENT_OPTIONS = List.of(
            new JobDocumentOption("RESUME", "이력서", "사진 포함 · 선택 시 필수", 1),
            new JobDocumentOption("DRIVER_LICENSE", "운전면허증 사본", "", 2),
            new JobDocumentOption("BUS_DRIVER_LICENSE", "버스운전자격증 사본", "", 3),
            new JobDocumentOption("APTITUDE_RESULT", "운전정밀검사 판정표", "최근 1년 이내 발급본", 4),
            new JobDocumentOption("TRAINING_CERTIFICATE", "운수종사자 교육 확인서", "", 5),
            new JobDocumentOption("DRIVING_CAREER", "운전경력 증명서", "경찰청·정부24 발급 가능", 6),
            new JobDocumentOption("RESIDENT_REGISTRATION", "주민등록등본", "채용 확정 후 별도 요청", 7),
            new JobDocumentOption("MEDICAL_CERTIFICATE", "채용신체검사서", "채용 확정 후 별도 요청", 8),
            new JobDocumentOption("CAREER_CERTIFICATE", "경력증명서", "경력 지원자 선택 제출", 9));
    private static final Set<String> DOCUMENT_CODES = DOCUMENT_OPTIONS.stream()
            .map(JobDocumentOption::code)
            .collect(Collectors.toUnmodifiableSet());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final RecruitmentMapper recruitmentMapper;
    private final Path storageRoot;

    public RecruitmentService(RecruitmentMapper recruitmentMapper,
                              @Value("${app.upload.recruitment-dir}") String storageDirectory) {
        this.recruitmentMapper = recruitmentMapper;
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public Page<JobPostingItem> getPostingPage(String keyword, int page) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : "";
        PageRequest pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);
        long total = recruitmentMapper.countPostings(normalized);
        List<JobPostingItem> content = recruitmentMapper.findPostingPage(
                        normalized, PAGE_SIZE, pageable.getOffset())
                .stream().map(this::toItem).toList();
        return new PageImpl<>(content, pageable, total);
    }

    public JobPostingDetail getPostingDetail(Long postingNo) {
        return toDetail(getRequiredPosting(postingNo));
    }

    public List<JobDocumentOption> getDocumentOptions() {
        return DOCUMENT_OPTIONS;
    }

    @Transactional
    public Long createPosting(String administratorId, JobPostingRequest request) {
        validatePostingSchedule(request);
        JobPosting posting = new JobPosting();
        posting.setTitle(request.getTitle().trim());
        posting.setContent(request.getContent().trim());
        posting.setDeadline(request.isAlwaysOpen() ? null : request.getDeadline());
        posting.setAlwaysOpen(request.isAlwaysOpen() ? "Y" : "N");
        posting.setStatus(request.isActive() ? "Y" : "N");
        posting.setCreatedBy(administratorId);
        recruitmentMapper.insertPosting(posting);
        savePostingDocuments(posting.getPostingNo(), request.getDocumentTypes());
        return posting.getPostingNo();
    }

    public JobPostingRequest getPostingRequest(Long postingNo) {
        JobPosting posting = getRequiredPosting(postingNo);
        JobPostingRequest request = new JobPostingRequest();
        request.setTitle(posting.getTitle());
        request.setContent(posting.getContent());
        request.setDeadline(posting.getDeadline());
        request.setAlwaysOpen("Y".equals(posting.getAlwaysOpen()));
        request.setActive("Y".equals(posting.getStatus()));
        request.setDocumentTypes(recruitmentMapper.findPostingDocumentTypes(postingNo));
        return request;
    }

    @Transactional
    public void updatePosting(Long postingNo, JobPostingRequest request) {
        validatePostingSchedule(request);
        JobPosting posting = getRequiredPosting(postingNo);
        posting.setTitle(request.getTitle().trim());
        posting.setContent(request.getContent().trim());
        posting.setDeadline(request.isAlwaysOpen() ? null : request.getDeadline());
        posting.setAlwaysOpen(request.isAlwaysOpen() ? "Y" : "N");
        posting.setStatus(request.isActive() ? "Y" : "N");
        if (recruitmentMapper.updatePosting(posting) != 1) {
            throw new IllegalArgumentException("채용공고를 수정하지 못했습니다.");
        }
        recruitmentMapper.deletePostingDocuments(postingNo);
        savePostingDocuments(postingNo, request.getDocumentTypes());
    }

    @Transactional
    public void deletePosting(Long postingNo) {
        getRequiredPosting(postingNo);
        long applicationCount = recruitmentMapper.countApplications(postingNo);
        if (applicationCount > 0) {
            throw new IllegalStateException("지원자가 있는 채용공고는 삭제할 수 없습니다. 접수를 마감해 보존해 주세요.");
        }
        if (recruitmentMapper.deletePosting(postingNo) != 1) {
            throw new IllegalArgumentException("채용공고를 삭제하지 못했습니다.");
        }
    }

    @Transactional
    public void changePostingStatus(Long postingNo, boolean active) {
        getRequiredPosting(postingNo);
        if (recruitmentMapper.updatePostingStatus(postingNo, active ? "Y" : "N") != 1) {
            throw new IllegalArgumentException("채용공고 상태를 변경하지 못했습니다.");
        }
    }

    @Transactional
    public Long apply(Long postingNo, JobApplicationRequest request) {
        JobPosting posting = getRequiredPosting(postingNo);
        if (!isOpen(posting)) throw new IllegalArgumentException("접수가 마감된 채용공고입니다.");

        Set<String> configuredTypes = new HashSet<>(
                recruitmentMapper.findPostingDocumentTypes(postingNo));
        List<SelectedDocument> documents = selectedDocuments(request);
        for (SelectedDocument document : documents) {
            if (!configuredTypes.contains(document.type())) {
                throw new ApplicationFileValidationException(
                        document.fieldName(),
                        document.label() + ": 해당 공고에서 요청하지 않는 서류입니다.",
                        null);
            }
        }
        if (configuredTypes.contains("RESUME")
                && documents.stream().noneMatch(document -> "RESUME".equals(document.type()))) {
            throw new ApplicationFileValidationException(
                    "resume", "사진이 포함된 이력서는 필수 제출 서류입니다.", null);
        }
        long totalBytes = 0;
        for (SelectedDocument document : documents) {
            try {
                UploadValidation.validate(document.file(), MAX_FILE_SIZE, ALLOWED_EXTENSIONS);
            } catch (IllegalArgumentException exception) {
                throw new ApplicationFileValidationException(
                        document.fieldName(),
                        document.label() + ": " + exception.getMessage(),
                        exception);
            }
            totalBytes += document.file().getSize();
            if (totalBytes > MAX_TOTAL_SIZE) {
                throw new IllegalArgumentException("전체 첨부파일은 40MB 이하여야 합니다.");
            }
        }

        JobApplication application = new JobApplication();
        application.setPostingNo(postingNo);
        application.setApplicantName(request.getApplicantName().trim());
        application.setPhone(request.getPhone().trim());
        application.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
        application.setApplicantType(request.getApplicantType());
        application.setMemo(StringUtils.hasText(request.getMemo()) ? request.getMemo().trim() : "");
        application.setConsented(request.isConsented() ? "Y" : "N");
        recruitmentMapper.insertApplication(application);

        saveDocuments(application.getApplicationNo(), documents);
        return application.getApplicationNo();
    }

    public List<JobApplicationItem> getApplications(Long postingNo) {
        getRequiredPosting(postingNo);
        return recruitmentMapper.findApplications(postingNo).stream()
                .map(application -> new JobApplicationItem(
                        application.getApplicationNo(),
                        application.getApplicantName(),
                        application.getPhone(),
                        application.getEmail(),
                        "EXPERIENCED".equals(application.getApplicantType()) ? "경력" : "신입·자격 1년 미만",
                        application.getMemo(),
                        application.getAppliedAt().format(DATE_TIME),
                        recruitmentMapper.findApplicationFiles(application.getApplicationNo()).size()
                )).toList();
    }

    public String archiveFilename(Long applicationNo) {
        JobApplication application = getRequiredApplication(applicationNo);
        String safeName = application.getApplicantName().replaceAll("[^가-힣A-Za-z0-9_-]", "_");
        if (safeName.isBlank()) safeName = "지원자";
        return safeName + "_" + application.getAppliedAt().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE) + ".zip";
    }

    public void writeApplicationArchive(Long applicationNo, OutputStream output) throws IOException {
        getRequiredApplication(applicationNo);
        List<JobApplicationFile> files = recruitmentMapper.findApplicationFiles(applicationNo);
        if (files.isEmpty()) throw new IllegalArgumentException("다운로드할 지원서 파일이 없습니다.");

        Set<String> usedNames = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (JobApplicationFile file : files) {
                Path path = storageRoot.resolve(file.getFilePath()).normalize();
                UploadValidation.ensureInside(storageRoot, path);
                if (!Files.isRegularFile(path)) throw new IllegalArgumentException("저장된 지원서 파일을 찾을 수 없습니다.");
                String entryName = uniqueEntryName(documentLabel(file.getDocumentType()) + "_" + file.getOriginalName(), usedNames);
                zip.putNextEntry(new ZipEntry(entryName));
                Files.copy(path, zip);
                zip.closeEntry();
            }
            zip.finish();
        }
    }

    private void saveDocuments(Long applicationNo, List<SelectedDocument> documents) {
        Path directory = storageRoot.resolve(String.valueOf(applicationNo)).normalize();
        try {
            UploadValidation.ensureInside(storageRoot, directory);
            Files.createDirectories(directory);
            for (SelectedDocument document : documents) {
                String originalName = UploadValidation.filename(document.file());
                String extension = UploadValidation.extension(originalName);
                String storedName = UUID.randomUUID() + "." + extension;
                Path destination = directory.resolve(storedName).normalize();
                UploadValidation.copy(document.file(), storageRoot, destination);

                JobApplicationFile file = new JobApplicationFile();
                file.setApplicationNo(applicationNo);
                file.setDocumentType(document.type());
                file.setOriginalName(originalName);
                file.setStoredName(storedName);
                file.setFilePath(storageRoot.relativize(destination).toString().replace('\\', '/'));
                file.setContentType(UploadValidation.contentType(extension));
                file.setFileSize(document.file().getSize());
                recruitmentMapper.insertApplicationFile(file);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("지원서 파일을 저장하지 못했습니다.", exception);
        }
    }

    private List<SelectedDocument> selectedDocuments(JobApplicationRequest request) {
        List<SelectedDocument> documents = new ArrayList<>();
        addIfSelected(documents, "resume", "RESUME", "이력서", request.getResume());
        addIfSelected(documents, "driverLicense", "DRIVER_LICENSE", "운전면허증 사본", request.getDriverLicense());
        addIfSelected(documents, "busDriverLicense", "BUS_DRIVER_LICENSE", "버스운전자격증 사본", request.getBusDriverLicense());
        addIfSelected(documents, "aptitudeResult", "APTITUDE_RESULT", "운전정밀검사 판정표", request.getAptitudeResult());
        addIfSelected(documents, "trainingCertificate", "TRAINING_CERTIFICATE", "운수종사자 교육 확인서", request.getTrainingCertificate());
        addIfSelected(documents, "drivingCareer", "DRIVING_CAREER", "운전경력 증명서", request.getDrivingCareer());
        addIfSelected(documents, "careerCertificate", "CAREER_CERTIFICATE", "경력증명서", request.getCareerCertificate());
        return documents;
    }

    private void addIfSelected(List<SelectedDocument> documents, String fieldName,
                               String type, String label, MultipartFile file) {
        if (UploadValidation.isSelected(file)) {
            documents.add(new SelectedDocument(fieldName, type, label, file));
        }
    }

    private void savePostingDocuments(Long postingNo, List<String> requestedTypes) {
        Set<String> normalized = requestedTypes == null
                ? Set.of()
                : requestedTypes.stream()
                        .filter(DOCUMENT_CODES::contains)
                        .collect(Collectors.toSet());
        for (JobDocumentOption option : DOCUMENT_OPTIONS) {
            if (normalized.contains(option.code())) {
                recruitmentMapper.insertPostingDocument(
                        postingNo, option.code(), option.displayOrder());
            }
        }
    }

    private void validatePostingSchedule(JobPostingRequest request) {
        if (!request.isDeadlineConfigurationValid()) {
            throw new IllegalArgumentException(
                    "상시 모집이 아니면 오늘 이후의 지원 마감일을 선택해 주세요.");
        }
    }

    private JobPosting getRequiredPosting(Long postingNo) {
        return recruitmentMapper.findPosting(postingNo)
                .orElseThrow(() -> new IllegalArgumentException("채용공고를 찾을 수 없습니다."));
    }

    private JobApplication getRequiredApplication(Long applicationNo) {
        return recruitmentMapper.findApplication(applicationNo)
                .orElseThrow(() -> new IllegalArgumentException("지원서를 찾을 수 없습니다."));
    }

    private JobPostingItem toItem(JobPosting posting) {
        boolean open = isOpen(posting);
        return new JobPostingItem(
                posting.getPostingNo(), posting.getTitle(), deadlineLabel(posting),
                open ? "접수중" : "마감", open ? "open" : "closed",
                recruitmentMapper.countApplications(posting.getPostingNo()),
                posting.getCreatedAt().toLocalDate().format(DATE));
    }

    private JobPostingDetail toDetail(JobPosting posting) {
        boolean open = isOpen(posting);
        return new JobPostingDetail(
                posting.getPostingNo(), posting.getTitle(), posting.getContent(),
                deadlineLabel(posting), open ? "접수중" : "마감",
                open ? "open" : "closed", open,
                List.copyOf(recruitmentMapper.findPostingDocumentTypes(posting.getPostingNo())));
    }

    private boolean isOpen(JobPosting posting) {
        return "Y".equals(posting.getStatus())
                && ("Y".equals(posting.getAlwaysOpen())
                    || (posting.getDeadline() != null
                        && !posting.getDeadline().isBefore(LocalDate.now())));
    }

    private String deadlineLabel(JobPosting posting) {
        return "Y".equals(posting.getAlwaysOpen())
                ? "상시 모집"
                : posting.getDeadline() == null
                    ? "마감일 미지정"
                    : posting.getDeadline().format(DATE);
    }

    private String documentLabel(String type) {
        return switch (type) {
            case "RESUME" -> "이력서";
            case "DRIVER_LICENSE" -> "운전면허증";
            case "BUS_DRIVER_LICENSE" -> "버스운전자격증";
            case "APTITUDE_RESULT" -> "운전정밀검사";
            case "TRAINING_CERTIFICATE" -> "운수종사자교육";
            case "DRIVING_CAREER" -> "운전경력증명서";
            case "CAREER_CERTIFICATE" -> "경력증명서";
            default -> "지원서류";
        };
    }

    private String uniqueEntryName(String requested, Set<String> usedNames) {
        String name = requested;
        int sequence = 2;
        while (!usedNames.add(name.toLowerCase(Locale.ROOT))) {
            int dot = requested.lastIndexOf('.');
            name = dot > 0
                    ? requested.substring(0, dot) + "_" + sequence + requested.substring(dot)
                    : requested + "_" + sequence;
            sequence++;
        }
        return name;
    }

    private record SelectedDocument(String fieldName, String type, String label, MultipartFile file) { }
}
