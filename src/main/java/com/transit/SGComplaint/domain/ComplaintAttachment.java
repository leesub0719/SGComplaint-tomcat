package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class ComplaintAttachment {
    private Long attachmentNo;
    private Long complaintNo;
    private String originalName;
    private String storedName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;

    protected ComplaintAttachment() {
    }

    public static ComplaintAttachment create(
            Long complaintNo,
            String originalName,
            String storedName,
            String filePath,
            String contentType,
            long fileSize) {

        ComplaintAttachment attachment = new ComplaintAttachment();
        attachment.complaintNo = complaintNo;
        attachment.originalName = originalName;
        attachment.storedName = storedName;
        attachment.filePath = filePath;
        attachment.contentType = contentType;
        attachment.fileSize = fileSize;
        return attachment;
    }
    private void onCreate() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getAttachmentNo() { return attachmentNo; }
    public Long getComplaintNo() { return complaintNo; }
    public String getOriginalName() { return originalName; }
    public String getFilePath() { return filePath; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
}
