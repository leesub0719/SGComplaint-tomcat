package com.transit.SGComplaint.domain;

import java.time.LocalDateTime;

public class JobApplication {
    private Long applicationNo;
    private Long postingNo;
    private String postingTitle;
    private String applicantName;
    private String phone;
    private String email;
    private String applicantType;
    private String memo;
    private String consented;
    private LocalDateTime appliedAt;

    public Long getApplicationNo() { return applicationNo; }
    public void setApplicationNo(Long applicationNo) { this.applicationNo = applicationNo; }
    public Long getPostingNo() { return postingNo; }
    public void setPostingNo(Long postingNo) { this.postingNo = postingNo; }
    public String getPostingTitle() { return postingTitle; }
    public void setPostingTitle(String postingTitle) { this.postingTitle = postingTitle; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getApplicantType() { return applicantType; }
    public void setApplicantType(String applicantType) { this.applicantType = applicantType; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getConsented() { return consented; }
    public void setConsented(String consented) { this.consented = consented; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
