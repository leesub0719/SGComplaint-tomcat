package com.transit.SGComplaint.service;

import com.transit.SGComplaint.domain.ComplaintSmsNotification;
import com.transit.SGComplaint.event.ComplaintAnsweredEvent;
import com.transit.SGComplaint.mapper.ComplaintSmsNotificationMapper;
import com.transit.SGComplaint.sms.SmsSender;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class ComplaintSmsNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(ComplaintSmsNotificationService.class);
    private static final int RETRY_BATCH_SIZE = 30;
    private static final int MAX_ERROR_LENGTH = 500;

    private final ComplaintSmsNotificationMapper mapper;
    private final SmsSender smsSender;
    private final boolean enabled;

    public ComplaintSmsNotificationService(
            ComplaintSmsNotificationMapper mapper,
            SmsSender smsSender,
            @Value("${sms.complaint-notification.enabled:false}") boolean enabled) {
        this.mapper = mapper;
        this.smsSender = smsSender;
        this.enabled = enabled;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAnswerRegistered(ComplaintAnsweredEvent event) {
        if (!enabled) {
            log.info("민원 답변 SMS 알림이 비활성화되어 있습니다. complaintNo={}",
                    event.complaintNo());
            return;
        }

        String phone;
        try {
            phone = PhoneNumberUtils.normalize(event.recipientPhone());
        } catch (PhoneVerificationException exception) {
            log.warn("민원 답변 SMS 수신번호가 유효하지 않습니다. complaintNo={}",
                    event.complaintNo());
            return;
        }

        String message = "[서경버스] 민원 " + event.complaintNo()
                + "번 답변이 등록되었습니다. 사이트에서 확인해 주세요.";
        ComplaintSmsNotification notification =
                ComplaintSmsNotification.answerRegistered(
                        event.complaintNo(), phone, message);

        try {
            if (mapper.insertIfAbsent(notification) == 1) {
                sendClaimed(notification.getNotificationNo());
            }
        } catch (RuntimeException exception) {
            // 원 답변 트랜잭션은 이미 커밋되었습니다. 알림 저장 장애를
            // 관리자 답변 실패로 오인시키지 않고 별도로 기록합니다.
            log.error("민원 답변 SMS 알림 처리 중 오류가 발생했습니다. complaintNo={}",
                    event.complaintNo(), exception);
        }
    }

    @Scheduled(
            fixedDelayString = "${sms.complaint-notification.retry-fixed-delay-ms:300000}",
            initialDelayString = "${sms.complaint-notification.retry-initial-delay-ms:60000}")
    @Transactional
    public void retryFailedNotifications() {
        if (!enabled) {
            return;
        }
        mapper.recoverStaleSending(LocalDateTime.now().minusMinutes(10));
        List<Long> candidates = mapper.findRetryCandidates(RETRY_BATCH_SIZE);
        for (Long notificationNo : candidates) {
            sendClaimed(notificationNo);
        }
    }

    private void sendClaimed(Long notificationNo) {
        if (notificationNo == null || mapper.claim(notificationNo) != 1) {
            return;
        }

        ComplaintSmsNotification notification = mapper.findById(notificationNo)
                .orElse(null);
        if (notification == null) {
            return;
        }

        try {
            smsSender.sendMessage(
                    notification.getRecipientPhone(),
                    notification.getMessageText());
            mapper.markSent(notificationNo);
            log.info("민원 답변 SMS 발송 완료. complaintNo={}, notificationNo={}",
                    notification.getComplaintNo(), notificationNo);
        } catch (RuntimeException exception) {
            mapper.markFailed(notificationNo, limitedError(exception));
            log.warn("민원 답변 SMS 발송 실패. complaintNo={}, notificationNo={}, attempt={}",
                    notification.getComplaintNo(), notificationNo,
                    notification.getAttemptCount());
        }
    }

    private String limitedError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }
}
