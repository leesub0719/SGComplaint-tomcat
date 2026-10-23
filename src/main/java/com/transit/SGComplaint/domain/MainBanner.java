package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class MainBanner {
    private Long bannerNo;
    private String originalName;
    private String storedName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private Integer displayOrder;
    private String createdBy;
    private LocalDateTime createdAt;

    protected MainBanner() {
    }

    public static MainBanner create(
            String originalName,
            String storedName,
            String filePath,
            String contentType,
            long fileSize,
            int displayOrder,
            String createdBy) {
        MainBanner banner = new MainBanner();
        banner.originalName = originalName;
        banner.storedName = storedName;
        banner.filePath = filePath;
        banner.contentType = contentType;
        banner.fileSize = fileSize;
        banner.displayOrder = displayOrder;
        banner.createdBy = createdBy;
        return banner;
    }
    private void onCreate() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getBannerNo() { return bannerNo; }
    public String getOriginalName() { return originalName; }
    public String getFilePath() { return filePath; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public Integer getDisplayOrder() { return displayOrder; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
