package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class Notice {
    private Long noticeNo;
    private Long adminEmpNo;
    private String adminName;
    private String category;
    private String title;
    private String content;
    private String pinned;
    private String popup;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Notice() {
    }

    public static Notice create(
            Employee administrator,
            String category,
            String title,
            String content,
            boolean pinned,
            boolean popup) {
        Notice notice = new Notice();
        notice.adminEmpNo = administrator.getEmpNo();
        notice.adminName = administrator.getEmpName();
        notice.category = category;
        notice.title = title.trim();
        notice.content = content.trim();
        notice.pinned = pinned ? "Y" : "N";
        notice.popup = popup ? "Y" : "N";
        notice.viewCount = 0L;
        return notice;
    }
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        createdAt = now;
        updatedAt = now;
    }
    private void onUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public void increaseViewCount() {
        viewCount = viewCount == null ? 1L : viewCount + 1L;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void changeInformation(
            Employee administrator,
            String category,
            String title,
            String content,
            boolean pinned,
            boolean popup) {
        this.adminEmpNo = administrator.getEmpNo();
        this.adminName = administrator.getEmpName();
        this.category = category;
        this.title = title.trim();
        this.content = content.trim();
        this.pinned = pinned ? "Y" : "N";
        this.popup = popup ? "Y" : "N";
    }

    public void changePopup(boolean popup) {
        this.popup = popup ? "Y" : "N";
    }

    public Long getNoticeNo() { return noticeNo; }
    public String getAdminName() { return adminName; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getPinned() { return pinned; }
    public String getPopup() { return popup; }
    public Long getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
