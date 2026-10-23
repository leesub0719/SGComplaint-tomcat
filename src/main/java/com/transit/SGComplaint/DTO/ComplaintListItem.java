package com.transit.SGComplaint.DTO;

import java.util.List;

public class ComplaintListItem {

    private final Long complaintNo;
    private final String categoryCode;
    private final String categoryLabel;
    private final String title;
    private final String content;
    private final String statusCode;
    private final String statusLabel;
    private final String statusCssClass;
    private final String registeredDate;
    private final String registeredDateTime;
    private final String answerContent;
    private final List<ComplaintFileItem> answerAttachments;
    private final boolean editable;

    public ComplaintListItem(
            Long complaintNo,
            String categoryCode,
            String categoryLabel,
            String title,
            String content,
            String statusCode,
            String statusLabel,
            String statusCssClass,
            String registeredDate,
            String registeredDateTime,
            String answerContent,
            List<ComplaintFileItem> answerAttachments,
            boolean editable) {
        this.complaintNo = complaintNo;
        this.categoryCode = categoryCode;
        this.categoryLabel = categoryLabel;
        this.title = title;
        this.content = content;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.statusCssClass = statusCssClass;
        this.registeredDate = registeredDate;
        this.registeredDateTime = registeredDateTime;
        this.answerContent = answerContent;
        this.answerAttachments = List.copyOf(answerAttachments);
        this.editable = editable;
    }

    public Long getComplaintNo() { return complaintNo; }
    public String getCategoryCode() { return categoryCode; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getStatusCode() { return statusCode; }
    public String getStatusLabel() { return statusLabel; }
    public String getStatusCssClass() { return statusCssClass; }
    public String getRegisteredDate() { return registeredDate; }
    public String getRegisteredDateTime() { return registeredDateTime; }
    public String getAnswerContent() { return answerContent; }
    public List<ComplaintFileItem> getAnswerAttachments() { return answerAttachments; }
    public boolean isEditable() { return editable; }
}
