package com.transit.SGComplaint.domain;

import java.time.LocalDateTime;

public class ComplaintSmsNotification {

    public static final String TYPE_ANSWER_REGISTERED = "ANSWER_REGISTERED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENDING = "SENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    private Long notificationNo;
    private Long complaintNo;
    private String notificationType;
    private String recipientPhone;
    private String messageText;
    private String sendStatus;
    private int attemptCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;

    public static ComplaintSmsNotification answerRegistered(
            Long complaintNo,
            String recipientPhone,
            String messageText) {
        ComplaintSmsNotification notification = new ComplaintSmsNotification();
        notification.complaintNo = complaintNo;
        notification.notificationType = TYPE_ANSWER_REGISTERED;
        notification.recipientPhone = recipientPhone;
        notification.messageText = messageText;
        notification.sendStatus = STATUS_PENDING;
        return notification;
    }

    public Long getNotificationNo() { return notificationNo; }
    public Long getComplaintNo() { return complaintNo; }
    public String getNotificationType() { return notificationType; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getMessageText() { return messageText; }
    public String getSendStatus() { return sendStatus; }
    public int getAttemptCount() { return attemptCount; }
    public String getLastError() { return lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getSentAt() { return sentAt; }
}
