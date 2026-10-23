package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class ComplaintAnswer {
    private Long answerNo;
    private Long complaintNo;
    private Long adminEmpNo;
    private String adminName;
    private String answerContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ComplaintAnswer() {
    }

    public static ComplaintAnswer create(
            Long complaintNo,
            Employee administrator,
            String answerContent) {
        ComplaintAnswer answer = new ComplaintAnswer();
        answer.complaintNo = complaintNo;
        answer.adminEmpNo = administrator.getEmpNo();
        answer.adminName = administrator.getEmpName();
        answer.answerContent = answerContent.trim();
        return answer;
    }

    public void update(Employee administrator, String answerContent) {
        adminEmpNo = administrator.getEmpNo();
        adminName = administrator.getEmpName();
        this.answerContent = answerContent.trim();
    }
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        createdAt = now;
        updatedAt = now;
    }
    private void onUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getAnswerNo() { return answerNo; }
    public Long getComplaintNo() { return complaintNo; }
    public String getAdminName() { return adminName; }
    public String getAnswerContent() { return answerContent; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
