package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class ComplaintAnswerAttachment {
    private Long answerAttachmentNo;
    private Long answerNo;
    private String originalName;
    private String storedName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;

    protected ComplaintAnswerAttachment() {
    }

    public static ComplaintAnswerAttachment create(
            Long answerNo,
            String originalName,
            String storedName,
            String filePath,
            String contentType,
            long fileSize) {
        ComplaintAnswerAttachment attachment = new ComplaintAnswerAttachment();
        attachment.answerNo = answerNo;
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

    public Long getAnswerAttachmentNo() { return answerAttachmentNo; }
    public Long getAnswerNo() { return answerNo; }
    public String getOriginalName() { return originalName; }
    public String getFilePath() { return filePath; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
}
