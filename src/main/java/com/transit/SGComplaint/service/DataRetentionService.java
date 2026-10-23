package com.transit.SGComplaint.service;

import com.transit.SGComplaint.domain.ComplaintAnswerAttachment;
import com.transit.SGComplaint.domain.ComplaintAttachment;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import com.transit.SGComplaint.mapper.WithdrawalHistoryMapper;
import com.transit.SGComplaint.repository.ComplaintAnswerAttachmentRepository;
import com.transit.SGComplaint.repository.ComplaintAttachmentRepository;
import com.transit.SGComplaint.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@ConditionalOnProperty(name = "app.retention.cleanup-enabled", havingValue = "true")
public class DataRetentionService {
    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final EmployeeMapper employeeMapper;
    private final WithdrawalHistoryMapper withdrawalHistoryMapper;
    private final ComplaintRepository complaintRepository;
    private final ComplaintAttachmentRepository complaintAttachments;
    private final ComplaintAnswerAttachmentRepository answerAttachments;
    private final Path complaintRoot;
    private final Path answerRoot;
    private final int withdrawnDays;
    private final int attachmentDays;
    private final int batchSize;

    public DataRetentionService(EmployeeMapper employeeMapper,
                                WithdrawalHistoryMapper withdrawalHistoryMapper,
                                ComplaintRepository complaintRepository,
                                ComplaintAttachmentRepository complaintAttachments,
                                ComplaintAnswerAttachmentRepository answerAttachments,
                                @Value("${app.upload.complaint-dir}") String complaintDirectory,
                                @Value("${app.upload.answer-dir}") String answerDirectory,
                                @Value("${app.retention.withdrawn-personal-days:30}") int withdrawnDays,
                                @Value("${app.retention.complaint-attachment-days:365}") int attachmentDays,
                                @Value("${app.retention.batch-size:200}") int batchSize) {
        this.employeeMapper = employeeMapper;
        this.withdrawalHistoryMapper = withdrawalHistoryMapper;
        this.complaintRepository = complaintRepository;
        this.complaintAttachments = complaintAttachments;
        this.answerAttachments = answerAttachments;
        this.complaintRoot = Path.of(complaintDirectory).toAbsolutePath().normalize();
        this.answerRoot = Path.of(answerDirectory).toAbsolutePath().normalize();
        this.withdrawnDays = Math.max(1, withdrawnDays);
        this.attachmentDays = Math.max(1, attachmentDays);
        this.batchSize = Math.min(1000, Math.max(1, batchSize));
    }

    @Scheduled(cron = "${DATA_RETENTION_CLEANUP_CRON:0 30 3 * * *}", zone = "Asia/Seoul")
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime withdrawnCutoff = now.minusDays(withdrawnDays);
        LocalDateTime complaintCutoff = now.minusDays(attachmentDays);
        int employees = employeeMapper.anonymizeExpiredWithdrawn(withdrawnCutoff, batchSize);
        int histories = withdrawalHistoryMapper.deleteExpired(now, batchSize);
        int files = deleteComplaintFiles(complaintCutoff) + deleteAnswerFiles(complaintCutoff);
        int complaints = complaintRepository.anonymizeExpiredCompleted(complaintCutoff, batchSize);
        log.info("Retention cleanup completed: employees={}, withdrawalHistories={}, files={}, complaints={}",
                employees, histories, files, complaints);
    }

    private int deleteComplaintFiles(LocalDateTime cutoff) {
        int deleted = 0;
        for (ComplaintAttachment attachment : complaintAttachments.findExpiredCompleted(cutoff, batchSize)) {
            if (deleteFile(complaintRoot, attachment.getFilePath())) {
                deleted += complaintAttachments.deleteById(attachment.getAttachmentNo());
            }
        }
        return deleted;
    }

    private int deleteAnswerFiles(LocalDateTime cutoff) {
        int deleted = 0;
        for (ComplaintAnswerAttachment attachment : answerAttachments.findExpiredCompleted(cutoff, batchSize)) {
            if (deleteFile(answerRoot, attachment.getFilePath())) {
                deleted += answerAttachments.deleteById(attachment.getAnswerAttachmentNo());
            }
        }
        return deleted;
    }

    private boolean deleteFile(Path root, String relativePath) {
        try {
            Path file = root.resolve(relativePath).normalize();
            UploadValidation.ensureInside(root, file);
            Files.deleteIfExists(file);
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            log.error("Retention file deletion failed for a stored attachment path", exception);
            return false;
        }
    }
}
