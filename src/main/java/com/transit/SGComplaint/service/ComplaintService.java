package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.ComplaintCreateRequest;
import com.transit.SGComplaint.DTO.ComplaintFileItem;
import com.transit.SGComplaint.DTO.ComplaintListItem;
import com.transit.SGComplaint.DTO.ComplaintUpdateRequest;
import com.transit.SGComplaint.DTO.PublicComplaintDetail;
import com.transit.SGComplaint.DTO.PublicComplaintItem;
import com.transit.SGComplaint.DTO.StoredAttachment;
import com.transit.SGComplaint.domain.Complaint;
import com.transit.SGComplaint.domain.ComplaintAnswer;
import com.transit.SGComplaint.domain.ComplaintAnswerAttachment;
import com.transit.SGComplaint.domain.ComplaintAttachment;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.repository.ComplaintAttachmentRepository;
import com.transit.SGComplaint.repository.ComplaintAnswerAttachmentRepository;
import com.transit.SGComplaint.repository.ComplaintAnswerRepository;
import com.transit.SGComplaint.repository.ComplaintRepository;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ComplaintService {

    private static final int MAX_FILE_COUNT = 5;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "pdf", "doc", "docx", "hwp", "hwpx"
    );
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final EmployeeMapper employeeRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final ComplaintAnswerRepository answerRepository;
    private final ComplaintAnswerAttachmentRepository answerAttachmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RichTextSanitizer richTextSanitizer;
    private final Path storageRoot;
    private final Path answerStorageRoot;

    public ComplaintService(
            EmployeeMapper employeeRepository,
            ComplaintRepository complaintRepository,
            ComplaintAttachmentRepository attachmentRepository,
            ComplaintAnswerRepository answerRepository,
            ComplaintAnswerAttachmentRepository answerAttachmentRepository,
            PasswordEncoder passwordEncoder,
            RichTextSanitizer richTextSanitizer,
            @Value("${app.upload.complaint-dir}") String storageDirectory,
            @Value("${app.upload.answer-dir}") String answerStorageDirectory) {
        this.employeeRepository = employeeRepository;
        this.complaintRepository = complaintRepository;
        this.attachmentRepository = attachmentRepository;
        this.answerRepository = answerRepository;
        this.answerAttachmentRepository = answerAttachmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.richTextSanitizer = richTextSanitizer;
        this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.answerStorageRoot = Path.of(answerStorageDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public Long createComplaint(String loginId, ComplaintCreateRequest request) {
        Employee employee = employeeRepository
                .findByEmpIdAndEmpStatus(loginId, "Y")
                .orElseThrow(() -> new ComplaintException(
                        "로그인 회원 정보를 찾을 수 없습니다. 다시 로그인해 주세요."
                ));

        List<MultipartFile> files = request.getAttachments().stream()
                .filter(UploadValidation::isSelected)
                .toList();
        validateFiles(files);

        String sanitizedContent = richTextSanitizer.sanitize(request.getContent());
        int contentLength = richTextSanitizer.plainTextLength(sanitizedContent);
        if (contentLength == 0) {
            throw new ComplaintException("민원 내용을 입력해 주세요.");
        }
        if (contentLength > 2000) {
            throw new ComplaintException("내용은 2,000자 이내로 입력해 주세요.");
        }

        Complaint complaint = Complaint.create(
                employee,
                request.getCategory(),
                request.getTitle(),
                sanitizedContent,
                passwordEncoder.encode(request.getPostPassword())
        );
        complaintRepository.saveAndFlush(complaint);

        if (!files.isEmpty()) {
            saveAttachments(complaint.getComplaintNo(), files);
        }
        return complaint.getComplaintNo();
    }

    public Page<PublicComplaintItem> getPublicComplaints(
            String category,
            String keyword,
            int pageNumber) {
        String normalizedCategory = normalizePublicCategory(category);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return complaintRepository.searchPublicComplaints(
                        normalizedCategory,
                        normalizedKeyword,
                        PageRequest.of(Math.max(pageNumber, 0), 10)
                )
                .map(complaint -> new PublicComplaintItem(
                        complaint.getComplaintNo(),
                        complaint.getCategory(),
                        categoryLabel(complaint.getCategory()),
                        complaint.getStatus().name(),
                        complaint.getStatus().getLabel(),
                        complaint.getTitle(),
                        StringUtils.hasText(complaint.getPassword()),
                        maskName(complaint.getEmpName()),
                        complaint.getCreatedAt().format(DATE_FORMATTER)
                ));
    }

    public boolean verifyPublicPassword(Long complaintNo, String password) {
        if (!StringUtils.hasText(password)) return false;
        Complaint complaint = getRequiredComplaint(complaintNo);
        return StringUtils.hasText(complaint.getPassword())
                && passwordEncoder.matches(password, complaint.getPassword());
    }

    public PublicComplaintDetail getPublicComplaint(Long complaintNo) {
        Complaint complaint = getRequiredComplaint(complaintNo);
        ComplaintAnswer answer = answerRepository.findByComplaintNo(complaintNo).orElse(null);
        List<ComplaintFileItem> answerFiles = answer == null
                ? List.of()
                : answerAttachmentRepository
                        .findByAnswerNoInOrderByAnswerAttachmentNoAsc(List.of(answer.getAnswerNo()))
                        .stream()
                        .map(attachment -> new ComplaintFileItem(
                                attachment.getOriginalName(),
                                formatFileSize(attachment.getFileSize()),
                                "/complaints/view/" + complaintNo
                                        + "/answer-attachments/"
                                        + attachment.getAnswerAttachmentNo()
                        ))
                        .toList();
        List<ComplaintFileItem> files = attachmentRepository
                .findByComplaintNoInOrderByAttachmentNoAsc(List.of(complaintNo))
                .stream()
                .map(attachment -> new ComplaintFileItem(
                        attachment.getOriginalName(),
                        formatFileSize(attachment.getFileSize()),
                        "/complaints/view/" + complaintNo + "/attachments/" + attachment.getAttachmentNo()
                ))
                .toList();
        return new PublicComplaintDetail(
                complaintNo,
                publicCategoryCode(complaint.getCategory()),
                categoryLabel(complaint.getCategory()),
                complaint.getStatus().name(),
                complaint.getStatus().getLabel(),
                complaint.getTitle(),
                maskName(complaint.getEmpName()),
                complaint.getCreatedAt().format(DATE_TIME_FORMATTER),
                richTextSanitizer.sanitize(complaint.getContent()),
                answer == null ? "" : answer.getAnswerContent(),
                answer == null ? "" : answer.getAdminName(),
                answer == null ? "" : answer.getUpdatedAt().format(DATE_TIME_FORMATTER),
                answerFiles,
                files
        );
    }

    public boolean canEditPublicComplaint(String loginId, Long complaintNo) {
        if (!StringUtils.hasText(loginId)) return false;
        return employeeRepository.findByEmpIdAndEmpStatus(loginId, "Y")
                .flatMap(employee -> complaintRepository.findById(complaintNo)
                        .map(complaint -> employee.getEmpNo().equals(complaint.getEmpNo())
                                && complaint.getStatus() == com.transit.SGComplaint.domain.ComplaintStatus.CHECKING))
                .orElse(false);
    }

    public StoredAttachment getPublicComplaintAnswerAttachment(
            Long complaintNo,
            Long attachmentNo) {
        ComplaintAnswerAttachment attachment = answerAttachmentRepository
                .findById(attachmentNo)
                .orElseThrow(() -> new ComplaintException(
                        "답변 첨부파일 정보를 찾을 수 없습니다."
                ));
        ComplaintAnswer answer = answerRepository.findById(attachment.getAnswerNo())
                .filter(item -> complaintNo.equals(item.getComplaintNo()))
                .orElseThrow(() -> new ComplaintException(
                        "해당 민원의 답변 첨부파일이 아닙니다."
                ));

        Path file = answerStorageRoot.resolve(attachment.getFilePath()).normalize();
        try { UploadValidation.ensureInside(answerStorageRoot, file); }
        catch (IllegalArgumentException exception) { throw new ComplaintException(exception.getMessage()); }
        if (!file.startsWith(answerStorageRoot) || !Files.isRegularFile(file)) {
            throw new ComplaintException("저장된 답변 첨부파일을 찾을 수 없습니다.");
        }
        return new StoredAttachment(
                file,
                attachment.getOriginalName(),
                attachment.getContentType()
        );
    }

    public StoredAttachment getPublicComplaintAttachment(Long complaintNo, Long attachmentNo) {
        ComplaintAttachment attachment = attachmentRepository.findById(attachmentNo)
                .filter(item -> complaintNo.equals(item.getComplaintNo()))
                .orElseThrow(() -> new ComplaintException("민원 첨부파일 정보를 찾을 수 없습니다."));
        Path file = storageRoot.resolve(attachment.getFilePath()).normalize();
        if (!file.startsWith(storageRoot) || !Files.isRegularFile(file)) {
            throw new ComplaintException("저장된 민원 첨부파일을 찾을 수 없습니다.");
        }
        return new StoredAttachment(file, attachment.getOriginalName(), attachment.getContentType());
    }

    private Complaint getRequiredComplaint(Long complaintNo) {
        return complaintRepository.findById(complaintNo)
                .orElseThrow(() -> new ComplaintException("민원 게시글을 찾을 수 없습니다."));
    }

    private String normalizePublicCategory(String category) {
        if (!StringUtils.hasText(category) || "ALL".equalsIgnoreCase(category)) return "";
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PRAISE", "COMPLAINT", "LOST").contains(normalized)) return "";
        return normalized;
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "PRAISE" -> "칭찬합니다";
            case "COMPLAINT", "DRIVER", "BUS" -> "불편합니다";
            case "LOST", "GENERAL" -> "분실물 문의";
            default -> "기타";
        };
    }

    private String publicCategoryCode(String category) {
        return switch (category) {
            case "PRAISE" -> "PRAISE";
            case "COMPLAINT", "DRIVER", "BUS" -> "COMPLAINT";
            case "LOST", "GENERAL" -> "LOST";
            default -> "COMPLAINT";
        };
    }

    private String maskName(String name) {
        if (!StringUtils.hasText(name)) return "*";
        int[] codePoints = name.trim().codePoints().toArray();
        if (codePoints.length == 1) return new String(codePoints, 0, 1);
        return new String(codePoints, 0, 1) + "*".repeat(codePoints.length - 1);
    }

    public Page<ComplaintListItem> getMemberComplaints(
            Long empNo,
            LocalDate startDate,
            LocalDate endDate,
            int pageNumber) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        int requestedPage = Math.max(0, pageNumber);
        Page<Complaint> complaintPage = complaintRepository.findMemberComplaints(
                empNo, startDateTime, endDateTime,
                PageRequest.of(requestedPage, 10));
        if (complaintPage.getTotalPages() > 0 && requestedPage >= complaintPage.getTotalPages()) {
            complaintPage = complaintRepository.findMemberComplaints(
                    empNo, startDateTime, endDateTime,
                    PageRequest.of(complaintPage.getTotalPages() - 1, 10));
        }
        List<Complaint> complaints = complaintPage.getContent();
        if (complaints.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(
                    List.of(), complaintPage.getPageable(), complaintPage.getTotalElements());
        }

        List<Long> complaintNumbers = complaints.stream()
                .map(Complaint::getComplaintNo)
                .toList();
        Map<Long, ComplaintAnswer> answersByComplaint = answerRepository
                .findByComplaintNoIn(complaintNumbers)
                .stream()
                .collect(Collectors.toMap(
                        ComplaintAnswer::getComplaintNo,
                        Function.identity()
                ));
        List<Long> answerNumbers = answersByComplaint.values().stream()
                .map(ComplaintAnswer::getAnswerNo)
                .toList();
        Map<Long, List<ComplaintAnswerAttachment>> attachmentsByAnswer =
                answerNumbers.isEmpty()
                        ? Map.of()
                        : answerAttachmentRepository
                                .findByAnswerNoInOrderByAnswerAttachmentNoAsc(answerNumbers)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        ComplaintAnswerAttachment::getAnswerNo
                                ));

        List<ComplaintListItem> items = complaints.stream()
                .map(complaint -> {
                    ComplaintAnswer answer = answersByComplaint.get(complaint.getComplaintNo());
                    List<ComplaintFileItem> answerFiles = answer == null
                            ? List.of()
                            : attachmentsByAnswer
                                    .getOrDefault(answer.getAnswerNo(), List.of())
                                    .stream()
                                    .map(this::toAnswerFileItem)
                                    .toList();
                    return toListItem(
                            complaint,
                            answer == null ? null : answer.getAnswerContent(),
                            answerFiles);
                })
                .toList();
        return new org.springframework.data.domain.PageImpl<>(
                items, complaintPage.getPageable(), complaintPage.getTotalElements());
    }

    @Transactional
    public void updateMemberComplaint(
            Long empNo,
            Long complaintNo,
            ComplaintUpdateRequest request) {
        Complaint complaint = getOwnedEditableComplaint(empNo, complaintNo);
        String sanitizedContent = richTextSanitizer.sanitize(request.getContent());
        complaint.updateContent(
                request.getCategory(),
                request.getTitle(),
                sanitizedContent);

        int updated = complaintRepository.updateMemberContent(
                complaintNo,
                empNo,
                complaint.getCategory(),
                complaint.getTitle(),
                complaint.getContent());
        if (updated != 1) {
            throw new IllegalStateException("민원 상태가 변경되어 수정할 수 없습니다.");
        }
    }

    @Transactional
    public void deleteMemberComplaint(Long empNo, Long complaintNo) {
        getOwnedEditableComplaint(empNo, complaintNo);
        List<ComplaintAttachment> attachments = attachmentRepository
                .findByComplaintNoInOrderByAttachmentNoAsc(List.of(complaintNo));

        int deleted = complaintRepository.deleteMemberComplaint(complaintNo, empNo);
        if (deleted != 1) {
            throw new IllegalStateException("민원 상태가 변경되어 삭제할 수 없습니다.");
        }

        for (ComplaintAttachment attachment : attachments) {
            Path file = storageRoot.resolve(attachment.getFilePath()).normalize();
            ensureInsideStorage(file);
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // DB 삭제는 완료한다. 남은 파일은 운영 정리 작업에서 제거할 수 있다.
            }
        }
        Path directory = storageRoot.resolve(String.valueOf(complaintNo)).normalize();
        ensureInsideStorage(directory);
        try {
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // 파일이 남아 있거나 이미 제거된 경우 DB 삭제 결과를 유지한다.
        }
    }

    private Complaint getOwnedEditableComplaint(Long empNo, Long complaintNo) {
        Complaint complaint = complaintRepository.findById(complaintNo)
                .orElseThrow(() -> new NoSuchElementException("민원 정보를 찾을 수 없습니다."));
        if (!empNo.equals(complaint.getEmpNo())) {
            throw new AccessDeniedException("본인이 작성한 민원만 변경할 수 있습니다.");
        }
        if (complaint.getStatus() != com.transit.SGComplaint.domain.ComplaintStatus.CHECKING) {
            throw new IllegalStateException("관리자가 확인한 민원은 수정하거나 삭제할 수 없습니다.");
        }
        return complaint;
    }

    public StoredAttachment getMemberAnswerAttachment(
            String loginId,
            Long attachmentNo) {
        Employee employee = employeeRepository
                .findByEmpIdAndEmpStatus(loginId, "Y")
                .orElseThrow(() -> new ComplaintException(
                        "로그인 회원 정보를 찾을 수 없습니다."
                ));
        ComplaintAnswerAttachment attachment = answerAttachmentRepository
                .findById(attachmentNo)
                .orElseThrow(() -> new ComplaintException(
                        "답변 첨부파일 정보를 찾을 수 없습니다."
                ));
        ComplaintAnswer answer = answerRepository.findById(attachment.getAnswerNo())
                .orElseThrow(() -> new ComplaintException(
                        "민원 답변 정보를 찾을 수 없습니다."
                ));
        Complaint complaint = complaintRepository.findById(answer.getComplaintNo())
                .orElseThrow(() -> new ComplaintException(
                        "민원 정보를 찾을 수 없습니다."
                ));

        if (!employee.getEmpNo().equals(complaint.getEmpNo())) {
            throw new ComplaintException("해당 첨부파일을 내려받을 권한이 없습니다.");
        }

        Path file = answerStorageRoot.resolve(attachment.getFilePath()).normalize();
        try { UploadValidation.ensureInside(answerStorageRoot, file); }
        catch (IllegalArgumentException exception) { throw new ComplaintException(exception.getMessage()); }
        if (!file.startsWith(answerStorageRoot) || !Files.isRegularFile(file)) {
            throw new ComplaintException("저장된 답변 첨부파일을 찾을 수 없습니다.");
        }
        return new StoredAttachment(
                file,
                attachment.getOriginalName(),
                attachment.getContentType()
        );
    }

    private ComplaintListItem toListItem(
            Complaint complaint,
            String answerContent,
            List<ComplaintFileItem> answerAttachments) {
        String cssClass = switch (complaint.getStatus()) {
            case CHECKING -> "status-checking";
            case PROCESSING -> "status-processing";
            case COMPLETED -> "status-completed";
        };

        return new ComplaintListItem(
                complaint.getComplaintNo(),
                complaint.getCategory(),
                categoryLabel(complaint.getCategory()),
                complaint.getTitle(),
                complaint.getContent(),
                complaint.getStatus().name(),
                complaint.getStatus().getLabel(),
                cssClass,
                complaint.getCreatedAt().format(DATE_FORMATTER),
                complaint.getCreatedAt().format(DATE_TIME_FORMATTER),
                answerContent,
                answerAttachments,
                complaint.getStatus() == com.transit.SGComplaint.domain.ComplaintStatus.CHECKING
        );
    }

    private ComplaintFileItem toAnswerFileItem(ComplaintAnswerAttachment attachment) {
        return new ComplaintFileItem(
                attachment.getOriginalName(),
                formatFileSize(attachment.getFileSize()),
                "/complaints/answer-attachments/" + attachment.getAnswerAttachmentNo()
        );
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files.size() > MAX_FILE_COUNT) throw new ComplaintException("첨부파일은 최대 5개입니다.");
        for (MultipartFile file : files) {
            try {
                UploadValidation.validate(file, MAX_FILE_SIZE, ALLOWED_EXTENSIONS);
            } catch (IllegalArgumentException exception) {
                throw new ComplaintException(exception.getMessage());
            }
        }
    }

    private void saveAttachments(Long complaintNo, List<MultipartFile> files) {
        Path complaintDirectory = storageRoot.resolve(String.valueOf(complaintNo)).normalize();
        ensureInsideStorage(complaintDirectory);
        List<Path> savedPaths = new ArrayList<>();

        try {
            Files.createDirectories(complaintDirectory);

            for (MultipartFile file : files) {
                String originalName = safeOriginalName(file);
                String extension = extensionOf(originalName);
                String storedName = UUID.randomUUID() + "." + extension;
                Path destination = complaintDirectory.resolve(storedName).normalize();
                ensureInsideStorage(destination);

                UploadValidation.copy(file, storageRoot, destination);
                savedPaths.add(destination);

                String relativePath = storageRoot.relativize(destination)
                        .toString().replace('\\', '/');
                ComplaintAttachment attachment = ComplaintAttachment.create(
                        complaintNo,
                        originalName,
                        storedName,
                        relativePath,
                        UploadValidation.contentType(extension),
                        file.getSize()
                );
                attachmentRepository.save(attachment);
            }
            attachmentRepository.flush();
        } catch (IOException | RuntimeException exception) {
            deleteSavedFiles(savedPaths);
            throw new ComplaintException(
                    "첨부파일 저장 중 오류가 발생했습니다. 다시 시도해 주세요.",
                    exception
            );
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

    private void ensureInsideStorage(Path path) {
        try { UploadValidation.ensureInside(storageRoot, path); } catch (IllegalArgumentException exception) {
            throw new ComplaintException(exception.getMessage());
        }
        if (!path.startsWith(storageRoot)) {
            throw new ComplaintException("올바르지 않은 첨부파일 저장 경로입니다.");
        }
    }

    private void deleteSavedFiles(List<Path> savedPaths) {
        for (Path savedPath : savedPaths) {
            try {
                Files.deleteIfExists(savedPath);
            } catch (IOException ignored) {
                // 원래 저장 오류를 유지합니다.
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(
                Locale.ROOT,
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
        );
    }
}
