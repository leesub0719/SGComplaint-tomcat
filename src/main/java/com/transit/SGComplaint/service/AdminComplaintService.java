package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.AdminAttachmentItem;
import com.transit.SGComplaint.DTO.AdminComplaintItem;
import com.transit.SGComplaint.DTO.StoredAttachment;
import com.transit.SGComplaint.domain.Complaint;
import com.transit.SGComplaint.domain.ComplaintAnswer;
import com.transit.SGComplaint.domain.ComplaintAnswerAttachment;
import com.transit.SGComplaint.domain.ComplaintAttachment;
import com.transit.SGComplaint.domain.ComplaintStatus;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.repository.ComplaintAnswerAttachmentRepository;
import com.transit.SGComplaint.repository.ComplaintAnswerRepository;
import com.transit.SGComplaint.repository.ComplaintAttachmentRepository;
import com.transit.SGComplaint.repository.ComplaintRepository;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminComplaintService {

    private static final int DASHBOARD_PAGE_SIZE = 10;
    private static final int MAX_ANSWER_FILE_COUNT = 5;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "pdf", "doc", "docx", "hwp", "hwpx"
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final ComplaintRepository complaintRepository;
    private final ComplaintAnswerRepository answerRepository;
    private final ComplaintAttachmentRepository complaintAttachmentRepository;
    private final ComplaintAnswerAttachmentRepository answerAttachmentRepository;
    private final EmployeeMapper employeeRepository;
    private final RichTextSanitizer richTextSanitizer;
    private final Path complaintStorageRoot;
    private final Path answerStorageRoot;

    public AdminComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintAnswerRepository answerRepository,
            ComplaintAttachmentRepository complaintAttachmentRepository,
            ComplaintAnswerAttachmentRepository answerAttachmentRepository,
            EmployeeMapper employeeRepository,
            RichTextSanitizer richTextSanitizer,
            @Value("${app.upload.complaint-dir}") String complaintStorageDirectory,
            @Value("${app.upload.answer-dir}") String answerStorageDirectory) {
        this.complaintRepository = complaintRepository;
        this.answerRepository = answerRepository;
        this.complaintAttachmentRepository = complaintAttachmentRepository;
        this.answerAttachmentRepository = answerAttachmentRepository;
        this.employeeRepository = employeeRepository;
        this.richTextSanitizer = richTextSanitizer;
        this.complaintStorageRoot = Path.of(complaintStorageDirectory).toAbsolutePath().normalize();
        this.answerStorageRoot = Path.of(answerStorageDirectory).toAbsolutePath().normalize();
    }

    public Page<AdminComplaintItem> getDashboardComplaints(
            ComplaintStatus status,
            int pageNumber) {
        Pageable pageable = PageRequest.of(Math.max(pageNumber, 0), DASHBOARD_PAGE_SIZE);
        Page<Complaint> complaints = status == null
                ? complaintRepository.findAllByOrderByCreatedAtDesc(pageable)
                : complaintRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return new PageImpl<>(
                toItems(complaints.getContent()),
                pageable,
                complaints.getTotalElements()
        );
    }

    public List<AdminComplaintItem> getComplaints(ComplaintStatus status) {
        List<Complaint> complaints = status == null
                ? complaintRepository.findAllByOrderByCreatedAtDesc()
                : complaintRepository.findByStatusOrderByCreatedAtDesc(status);
        return toItems(complaints);
    }

    public long countAllComplaints() { return complaintRepository.count(); }
    public long countComplaints(ComplaintStatus status) { return complaintRepository.countByStatus(status); }
    public long countActiveMembers() { return employeeRepository.countByEmpStatus("Y"); }

    @Transactional
    public void answerComplaint(
            String loginId,
            Long complaintNo,
            ComplaintStatus status,
            Long expectedVersion,
            String answerContent,
            List<MultipartFile> files) {

        Employee administrator = employeeRepository
                .findByEmpIdAndEmpStatus(loginId, "Y")
                .filter(Employee::hasAdminRole)
                .orElseThrow(() -> new ComplaintException("관리자 계정 정보를 확인할 수 없습니다."));
        Complaint complaint = complaintRepository.findById(complaintNo)
                .orElseThrow(() -> new ComplaintException("민원 정보를 찾을 수 없습니다."));

        String resolvedAnswerContent = StringUtils.hasText(answerContent)
                ? answerContent.trim()
                : status == null ? "" : status.getDefaultAnswer();
        validateAnswer(status, resolvedAnswerContent);
        if (expectedVersion == null) {
            throw new ComplaintException("민원 버전 정보가 없습니다. 목록을 새로고침해 주세요.");
        }
        List<MultipartFile> uploadFiles = files == null
                ? List.of()
                : files.stream().filter(UploadValidation::isSelected).toList();

        int updatedRows = complaintRepository.updateStatusWithVersion(
                complaintNo, status, expectedVersion);
        if (updatedRows == 0) {
            throw new ComplaintConflictException(
                    "다른 관리자가 먼저 이 민원을 수정했습니다. 목록을 새로고침한 뒤 다시 확인해 주세요.");
        }

        ComplaintAnswer answer = answerRepository.findByComplaintNo(complaintNo)
                .map(existingAnswer -> {
                    existingAnswer.update(administrator, resolvedAnswerContent);
                    return existingAnswer;
                })
                .orElseGet(() -> ComplaintAnswer.create(
                        complaintNo,
                        administrator,
                        resolvedAnswerContent
                ));
        answerRepository.saveAndFlush(answer);

        long existingFileCount = answerAttachmentRepository.countByAnswerNo(answer.getAnswerNo());
        validateAnswerFiles(uploadFiles, existingFileCount);
        if (!uploadFiles.isEmpty()) {
            saveAnswerAttachments(answer.getAnswerNo(), uploadFiles);
        }

    }

    public StoredAttachment getComplaintAttachment(Long attachmentNo) {
        ComplaintAttachment attachment = complaintAttachmentRepository.findById(attachmentNo)
                .orElseThrow(() -> new ComplaintException("첨부파일 정보를 찾을 수 없습니다."));
        return storedAttachment(
                complaintStorageRoot,
                attachment.getFilePath(),
                attachment.getOriginalName(),
                attachment.getContentType());
    }

    public StoredAttachment getAnswerAttachment(Long attachmentNo) {
        ComplaintAnswerAttachment attachment = answerAttachmentRepository.findById(attachmentNo)
                .orElseThrow(() -> new ComplaintException("답변 첨부파일 정보를 찾을 수 없습니다."));
        return storedAttachment(
                answerStorageRoot,
                attachment.getFilePath(),
                attachment.getOriginalName(),
                attachment.getContentType());
    }

    private StoredAttachment storedAttachment(
            Path root,
            String relativePath,
            String originalName,
            String contentType) {
        Path file = root.resolve(relativePath).normalize();
        ensureInsideStorage(root, file);
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new ComplaintException("저장된 첨부파일을 찾을 수 없습니다.");
        }
        return new StoredAttachment(file, originalName, contentType);
    }

    private List<AdminComplaintItem> toItems(List<Complaint> complaints) {
        if (complaints.isEmpty()) {
            return List.of();
        }

        List<Long> complaintNumbers = complaints.stream().map(Complaint::getComplaintNo).toList();
        Map<Long, ComplaintAnswer> answers = answerRepository.findByComplaintNoIn(complaintNumbers)
                .stream()
                .collect(Collectors.toMap(ComplaintAnswer::getComplaintNo, Function.identity()));
        Map<Long, List<ComplaintAttachment>> complaintAttachments = complaintAttachmentRepository
                .findByComplaintNoInOrderByAttachmentNoAsc(complaintNumbers)
                .stream()
                .collect(Collectors.groupingBy(ComplaintAttachment::getComplaintNo));

        List<Long> answerNumbers = answers.values().stream().map(ComplaintAnswer::getAnswerNo).toList();
        Map<Long, List<ComplaintAnswerAttachment>> answerAttachments = answerNumbers.isEmpty()
                ? Map.of()
                : answerAttachmentRepository.findByAnswerNoInOrderByAnswerAttachmentNoAsc(answerNumbers)
                        .stream()
                        .collect(Collectors.groupingBy(ComplaintAnswerAttachment::getAnswerNo));

        return complaints.stream()
                .map(complaint -> {
                    ComplaintAnswer answer = answers.get(complaint.getComplaintNo());
                    List<ComplaintAnswerAttachment> attachedAnswers = answer == null
                            ? List.of()
                            : answerAttachments.getOrDefault(answer.getAnswerNo(), List.of());
                    return toItem(
                            complaint,
                            answer,
                            complaintAttachments.getOrDefault(complaint.getComplaintNo(), List.of()),
                            attachedAnswers);
                })
                .toList();
    }

    private AdminComplaintItem toItem(
            Complaint complaint,
            ComplaintAnswer answer,
            List<ComplaintAttachment> complaintAttachments,
            List<ComplaintAnswerAttachment> answerAttachments) {

        String statusCssClass = switch (complaint.getStatus()) {
            case CHECKING -> "status-checking";
            case PROCESSING -> "status-processing";
            case COMPLETED -> "status-completed";
        };
        String categoryLabel = switch (complaint.getCategory()) {
            case "PRAISE" -> "칭찬합니다";
            case "COMPLAINT", "DRIVER", "BUS" -> "불편합니다";
            case "LOST", "GENERAL" -> "분실물 문의";
            default -> "기타";
        };

        List<AdminAttachmentItem> complaintFiles = complaintAttachments.stream()
                .map(attachment -> new AdminAttachmentItem(
                        attachment.getAttachmentNo(), attachment.getOriginalName(),
                        formatFileSize(attachment.getFileSize()),
                        "/admin/complaints/attachments/" + attachment.getAttachmentNo()))
                .toList();
        List<AdminAttachmentItem> answerFiles = answerAttachments.stream()
                .map(attachment -> new AdminAttachmentItem(
                        attachment.getAnswerAttachmentNo(), attachment.getOriginalName(),
                        formatFileSize(attachment.getFileSize()),
                        "/admin/answers/attachments/" + attachment.getAnswerAttachmentNo()))
                .toList();

        return new AdminComplaintItem(
                complaint.getComplaintNo(), complaint.getEmpId(), complaint.getEmpName(),
                complaint.getEmpPhone(), categoryLabel, complaint.getTitle(), richTextSanitizer.sanitize(complaint.getContent()),
                complaint.getStatus().name(), complaint.getStatus().getLabel(), statusCssClass,
                complaint.getVersion(),
                complaint.getCreatedAt().format(DATE_TIME_FORMATTER),
                answer == null ? "" : answer.getAnswerContent(),
                answer == null ? "" : answer.getAdminName(),
                answer == null ? "" : answer.getUpdatedAt().format(DATE_TIME_FORMATTER),
                complaintFiles, answerFiles);
    }

    private void validateAnswer(ComplaintStatus status, String answerContent) {
        if (status == null) {
            throw new ComplaintException("변경할 처리상태를 선택해 주세요.");
        }
        if (!StringUtils.hasText(answerContent)) {
            throw new ComplaintException("답변 내용을 입력해 주세요.");
        }
        if (answerContent.trim().length() > 3000) {
            throw new ComplaintException("답변은 3,000자 이내로 입력해 주세요.");
        }
    }

    private void validateAnswerFiles(List<MultipartFile> files, long existingFileCount) {
        if (existingFileCount + files.size() > MAX_ANSWER_FILE_COUNT) {
            throw new ComplaintException("답변 첨부파일은 기존 파일을 포함해 최대 5개입니다.");
        }
        for (MultipartFile file : files) {
            try {
                UploadValidation.validate(file, MAX_FILE_SIZE, ALLOWED_EXTENSIONS);
            } catch (IllegalArgumentException exception) {
                throw new ComplaintException(exception.getMessage());
            }
        }
    }

    private void saveAnswerAttachments(Long answerNo, List<MultipartFile> files) {
        Path answerDirectory = answerStorageRoot.resolve(String.valueOf(answerNo)).normalize();
        ensureInsideStorage(answerStorageRoot, answerDirectory);
        List<Path> savedPaths = new ArrayList<>();
        try {
            Files.createDirectories(answerDirectory);
            for (MultipartFile file : files) {
                String originalName = safeOriginalName(file);
                String extension = extensionOf(originalName);
                String storedName = UUID.randomUUID() + "." + extension;
                Path destination = answerDirectory.resolve(storedName).normalize();
                ensureInsideStorage(answerStorageRoot, destination);
                UploadValidation.copy(file, answerStorageRoot, destination);
                savedPaths.add(destination);

                String relativePath = answerStorageRoot.relativize(destination)
                        .toString().replace('\\', '/');
                answerAttachmentRepository.save(ComplaintAnswerAttachment.create(
                        answerNo, originalName, storedName, relativePath,
                        UploadValidation.contentType(extension), file.getSize()));
            }
            answerAttachmentRepository.flush();
        } catch (IOException | RuntimeException exception) {
            deleteSavedFiles(savedPaths);
            throw new ComplaintException("답변 첨부파일 저장 중 오류가 발생했습니다.", exception);
        }
    }

    private String safeOriginalName(MultipartFile file) {
        try {
            return UploadValidation.filename(file);
        } catch (IllegalArgumentException exception) {
            throw new ComplaintException(exception.getMessage());
        }
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void ensureInsideStorage(Path root, Path path) {
        try { UploadValidation.ensureInside(root, path); } catch (IllegalArgumentException exception) {
            throw new ComplaintException(exception.getMessage());
        }
        if (!path.startsWith(root)) {
            throw new ComplaintException("올바르지 않은 첨부파일 저장 경로입니다.");
        }
    }

    private void deleteSavedFiles(List<Path> paths) {
        for (Path path : paths) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // 원래 저장 오류를 유지합니다.
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
