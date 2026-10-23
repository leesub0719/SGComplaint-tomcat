package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class DdokBusGuideImage {
    private Long imageNo;
    private String originalName;
    private String storedName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;

    protected DdokBusGuideImage() {
    }

    public static DdokBusGuideImage create(
            String originalName,
            String storedName,
            String filePath,
            String contentType,
            long fileSize) {
        DdokBusGuideImage image = new DdokBusGuideImage();
        image.originalName = originalName;
        image.storedName = storedName;
        image.filePath = filePath;
        image.contentType = contentType;
        image.fileSize = fileSize;
        return image;
    }
    private void onCreate() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getImageNo() { return imageNo; }
    public String getOriginalName() { return originalName; }
    public String getStoredName() { return storedName; }
    public String getFilePath() { return filePath; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
