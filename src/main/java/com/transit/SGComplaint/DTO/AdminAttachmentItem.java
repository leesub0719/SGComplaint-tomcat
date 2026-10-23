package com.transit.SGComplaint.DTO;

public class AdminAttachmentItem {

    private final Long attachmentNo;
    private final String originalName;
    private final String formattedSize;
    private final String downloadUrl;

    public AdminAttachmentItem(
            Long attachmentNo,
            String originalName,
            String formattedSize,
            String downloadUrl) {
        this.attachmentNo = attachmentNo;
        this.originalName = originalName;
        this.formattedSize = formattedSize;
        this.downloadUrl = downloadUrl;
    }

    public Long getAttachmentNo() { return attachmentNo; }
    public String getOriginalName() { return originalName; }
    public String getFormattedSize() { return formattedSize; }
    public String getDownloadUrl() { return downloadUrl; }
}
