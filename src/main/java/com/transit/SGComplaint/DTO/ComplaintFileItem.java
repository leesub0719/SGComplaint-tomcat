package com.transit.SGComplaint.DTO;

public class ComplaintFileItem {

    private final String originalName;
    private final String formattedSize;
    private final String downloadUrl;

    public ComplaintFileItem(
            String originalName,
            String formattedSize,
            String downloadUrl) {
        this.originalName = originalName;
        this.formattedSize = formattedSize;
        this.downloadUrl = downloadUrl;
    }

    public String getOriginalName() { return originalName; }
    public String getFormattedSize() { return formattedSize; }
    public String getDownloadUrl() { return downloadUrl; }
}
