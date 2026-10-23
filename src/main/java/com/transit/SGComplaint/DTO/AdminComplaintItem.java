package com.transit.SGComplaint.DTO;

import java.util.List;

public class AdminComplaintItem {

    private final Long complaintNo;
    private final String memberId;
    private final String memberName;
    private final String memberPhone;
    private final String categoryLabel;
    private final String title;
    private final String content;
    private final String statusCode;
    private final String statusLabel;
    private final String statusCssClass;
    private final Long version;
    private final String registeredDateTime;
    private final String answerContent;
    private final String answerAdminName;
    private final String answeredDateTime;
    private final List<AdminAttachmentItem> complaintAttachments;
    private final List<AdminAttachmentItem> answerAttachments;

    public AdminComplaintItem(
            Long complaintNo, String memberId, String memberName,
            String memberPhone, String categoryLabel, String title,
            String content, String statusCode, String statusLabel,
            String statusCssClass, Long version, String registeredDateTime,
            String answerContent, String answerAdminName,
            String answeredDateTime,
            List<AdminAttachmentItem> complaintAttachments,
            List<AdminAttachmentItem> answerAttachments) {
        this.complaintNo = complaintNo;
        this.memberId = memberId;
        this.memberName = memberName;
        this.memberPhone = memberPhone;
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.content = content;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.statusCssClass = statusCssClass;
        this.version = version;
        this.registeredDateTime = registeredDateTime;
        this.answerContent = answerContent;
        this.answerAdminName = answerAdminName;
        this.answeredDateTime = answeredDateTime;
        this.complaintAttachments = List.copyOf(complaintAttachments);
        this.answerAttachments = List.copyOf(answerAttachments);
    }

    public Long getComplaintNo() { return complaintNo; }
    public String getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public String getMemberPhone() { return memberPhone; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStatusCode() { return statusCode; }
    public String getStatusLabel() { return statusLabel; }
    public String getStatusCssClass() { return statusCssClass; }
    public Long getVersion() { return version; }
    public String getRegisteredDateTime() { return registeredDateTime; }
    public String getAnswerContent() { return answerContent; }
    public String getAnswerAdminName() { return answerAdminName; }
    public String getAnsweredDateTime() { return answeredDateTime; }
    public List<AdminAttachmentItem> getComplaintAttachments() { return complaintAttachments; }
    public List<AdminAttachmentItem> getAnswerAttachments() { return answerAttachments; }
}
