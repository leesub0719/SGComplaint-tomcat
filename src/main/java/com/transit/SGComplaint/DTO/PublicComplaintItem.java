package com.transit.SGComplaint.DTO;

public record PublicComplaintItem(
        Long complaintNo,
        String categoryCode,
        String categoryLabel,
        String statusCode,
        String statusLabel,
        String title,
        boolean passwordProtected,
        String maskedWriterName,
        String registeredDate) {
}
