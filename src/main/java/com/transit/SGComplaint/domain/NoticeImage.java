package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class NoticeImage {
    private Long noticeImageNo;
    private Long noticeNo;
    private String originalName;
    private String storedName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;

    protected NoticeImage() {
    }

    public static NoticeImage create(
            Long noticeNo,
            String originalName,
            String storedName,
            String filePath,
            String contentType,
            long fileSize) {
        NoticeImage image = new NoticeImage();
        image.noticeNo = noticeNo;
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

    public Long getNoticeImageNo() { return noticeImageNo; }
    public Long getNoticeNo() { return noticeNo; }
    public String getOriginalName() { return originalName; }
    public String getFilePath() { return filePath; }
    public String getContentType() { return contentType; }
}
