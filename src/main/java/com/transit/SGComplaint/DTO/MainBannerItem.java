package com.transit.SGComplaint.DTO;

public class MainBannerItem {

    private final Long bannerNo;
    private final String originalName;
    private final String formattedSize;
    private final String createdBy;
    private final String createdDateTime;
    private final String imageUrl;

    public MainBannerItem(
            Long bannerNo,
            String originalName,
            String formattedSize,
            String createdBy,
            String createdDateTime,
            String imageUrl) {
        this.bannerNo = bannerNo;
        this.originalName = originalName;
        this.formattedSize = formattedSize;
        this.createdBy = createdBy;
        this.createdDateTime = createdDateTime;
        this.imageUrl = imageUrl;
    }

    public Long getBannerNo() { return bannerNo; }
    public String getOriginalName() { return originalName; }
    public String getFormattedSize() { return formattedSize; }
    public String getCreatedBy() { return createdBy; }
    public String getCreatedDateTime() { return createdDateTime; }
    public String getImageUrl() { return imageUrl; }
}
