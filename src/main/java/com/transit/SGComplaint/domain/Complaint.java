package com.transit.SGComplaint.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Complaint {
    private Long complaintNo;
    private Long empNo;
    private String empId;
    private String empName;
    private String empPhone;
    private String category;
    private String title;
    private String content;
    private String password;
    private ComplaintStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    protected Complaint() {
    }

    public static Complaint create(
            Employee employee,
            String category,
            String title,
            String content,
            String password) {

        Complaint complaint = new Complaint();
        complaint.empNo = employee.getEmpNo();
        complaint.empId = employee.getEmpId();
        complaint.empName = employee.getEmpName();
        complaint.empPhone = employee.getEmpPhone();
        complaint.category = category;
        complaint.title = title.trim();
        complaint.content = content;
        complaint.password = password;
        complaint.status = ComplaintStatus.CHECKING;
        complaint.version = 0L;
        return complaint;
    }

    private void onCreate() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        createdAt = now;
        updatedAt = now;
    }

    private void onUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getComplaintNo() { return complaintNo; }
    public Long getEmpNo() { return empNo; }
    public String getEmpId() { return empId; }
    public String getEmpName() { return empName; }
    public String getEmpPhone() { return empPhone; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getPassword() { return password; }
    public ComplaintStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    public void changeStatus(ComplaintStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("민원 처리상태는 필수입니다.");
        }
        this.status = status;
    }

    public void updateContent(
            String category,
            String title,
            String content) {
        if (status != ComplaintStatus.CHECKING) {
            throw new IllegalStateException(
                    "관리자가 확인한 민원은 수정할 수 없습니다.");
        }
        this.category = category;
        this.title = title.trim();
        this.content = content;
    }

}
