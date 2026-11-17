package com.transit.SGComplaint.domain;

import java.time.LocalDateTime;

public class JobApplicationFile {
    private Long fileNo;
    private Long applicationNo;
    private String documentType;
    private String originalName;
    private String storedName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;

    public Long getFileNo() { return fileNo; }
    public void setFileNo(Long fileNo) { this.fileNo = fileNo; }
    public Long getApplicationNo() { return applicationNo; }
    public void setApplicationNo(Long applicationNo) { this.applicationNo = applicationNo; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
