package com.transit.SGComplaint.DTO;

public class AdminMemberItem {

    private final Long empNo;
    private final String empId;
    private final String empName;
    private final String empEmail;
    private final String empPhone;
    private final String empAddress;
    private final String roleCode;
    private final String roleLabel;
    private final String statusCode;
    private final String statusLabel;
    private final String createdDate;
    private final String createdDateTime;
    private final String updatedDateTime;
    private final boolean currentAdministrator;
    private final String withdrawalOriginalId;

    public AdminMemberItem(
            Long empNo,
            String empId,
            String empName,
            String empEmail,
            String empPhone,
            String empAddress,
            String roleCode,
            String roleLabel,
            String statusCode,
            String statusLabel,
            String createdDate,
            String createdDateTime,
            String updatedDateTime,
            boolean currentAdministrator,
            String withdrawalOriginalId) {
        this.empNo = empNo;
        this.empId = empId;
        this.empName = empName;
        this.empEmail = empEmail;
        this.empPhone = empPhone;
        this.empAddress = empAddress;
        this.roleCode = roleCode;
        this.roleLabel = roleLabel;
        this.statusCode = statusCode;
        this.statusLabel = statusLabel;
        this.createdDate = createdDate;
        this.createdDateTime = createdDateTime;
        this.updatedDateTime = updatedDateTime;
        this.currentAdministrator = currentAdministrator;
        this.withdrawalOriginalId = withdrawalOriginalId;
    }

    public Long getEmpNo() { return empNo; }
    public String getEmpId() { return empId; }
    public String getEmpName() { return empName; }
    public String getEmpEmail() { return empEmail; }
    public String getEmpPhone() { return empPhone; }
    public String getEmpAddress() { return empAddress; }
    public String getRoleCode() { return roleCode; }
    public String getRoleLabel() { return roleLabel; }
    public String getStatusCode() { return statusCode; }
    public String getStatusLabel() { return statusLabel; }
    public String getCreatedDate() { return createdDate; }
    public String getCreatedDateTime() { return createdDateTime; }
    public String getUpdatedDateTime() { return updatedDateTime; }
    public boolean isCurrentAdministrator() { return currentAdministrator; }
    public String getWithdrawalOriginalId() { return withdrawalOriginalId; }
}
