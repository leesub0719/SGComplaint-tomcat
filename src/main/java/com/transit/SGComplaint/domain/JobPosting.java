package com.transit.SGComplaint.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class JobPosting {
    private Long postingNo;
    private String title;
    private String content;
    private LocalDate deadline;
    private String alwaysOpen;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getPostingNo() { return postingNo; }
    public void setPostingNo(Long postingNo) { this.postingNo = postingNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getAlwaysOpen() { return alwaysOpen; }
    public void setAlwaysOpen(String alwaysOpen) { this.alwaysOpen = alwaysOpen; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
