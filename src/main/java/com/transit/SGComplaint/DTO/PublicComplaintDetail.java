package com.transit.SGComplaint.DTO;

import java.util.List;

public record PublicComplaintDetail(
        Long complaintNo,
        String categoryCode,
        String categoryLabel,
        String statusCode,
        String statusLabel,
        String title,
        String maskedWriterName,
        String registeredDateTime,
        String content,
        String answerContent,
        String answerAdminName,
        String answeredDateTime,
        List<ComplaintFileItem> answerAttachments,
        List<ComplaintFileItem> attachments) {
}
