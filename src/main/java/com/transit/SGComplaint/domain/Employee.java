package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class Employee {
    private Long empNo;
    private String empId;
    private String empPassword;
    private String empName;
    private String empEmail;
    private String empPhone;
    private String empAddress;
    private String empRole;
    private String empStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String withdrawalOriginalId;

    protected Employee() {
    }

    public static Employee createUser(
            String empId,
            String encodedPassword,
            String empName,
            String empEmail,
            String empPhone,
            String empAddress) {

        Employee employee = new Employee();
        employee.empId = empId;
        employee.empPassword = encodedPassword;
        employee.empName = empName;
        employee.empEmail = empEmail;
        employee.empPhone = empPhone;
        employee.empAddress = empAddress;
        employee.empRole = "U";
        employee.empStatus = "Y";
        return employee;
    }

    public static Employee createAdmin(
            String empId,
            String encodedPassword,
            String empName,
            String empEmail,
            String empPhone,
            String empAddress) {

        Employee employee = createUser(
                empId, encodedPassword, empName,
                empEmail, empPhone, empAddress);
        employee.empRole = "A";
        return employee;
    }
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS);
        createdAt = now;
        updatedAt = now;
    }
    private void onUpdate() {
        updatedAt = LocalDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getEmpNo() { return empNo; }
    public String getEmpId() { return empId; }
    public String getEmpPassword() { return empPassword; }
    public String getEmpName() { return empName; }
    public String getEmpEmail() { return empEmail; }
    public String getEmpPhone() { return empPhone; }
    public String getEmpAddress() { return empAddress; }
    public String getEmpRole() { return empRole; }
    public String getEmpStatus() { return empStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getWithdrawalOriginalId() { return withdrawalOriginalId; }

    public void changeRole(String role) {
        if (!"U".equals(role) && !"A".equals(role) && !"M".equals(role)) {
            throw new IllegalArgumentException("회원 권한은 U, A 또는 M만 가능합니다.");
        }
        this.empRole = role;
    }

    public boolean hasAdminRole() {
        return "A".equals(empRole) || "M".equals(empRole);
    }

    public boolean isMaster() {
        return "M".equals(empRole);
    }

    public void changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("암호화된 비밀번호가 필요합니다.");
        }
        this.empPassword = encodedPassword;
    }

    public void updateProfile(
            String encodedPassword,
            String email,
            String phone,
            String address) {
        if (encodedPassword != null && !encodedPassword.isBlank()) {
            changePassword(encodedPassword);
        }
        this.empEmail = email;
        this.empPhone = phone;
        this.empAddress = address;
    }

    public void withdraw(String withdrawnId) {
        if (withdrawnId == null || withdrawnId.isBlank()) {
            throw new IllegalArgumentException("탈퇴 회원 식별값이 필요합니다.");
        }
        this.empId = withdrawnId;
        this.empStatus = "N";
    }
}
