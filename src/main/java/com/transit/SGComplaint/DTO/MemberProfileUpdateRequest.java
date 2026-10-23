package com.transit.SGComplaint.DTO;

import com.transit.SGComplaint.domain.Employee;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.regex.Matcher;

public class MemberProfileUpdateRequest {

    @Pattern(
        regexp = "^$|^(?=.*[A-Za-z])(?=.*[0-9]).{8,72}$",
        message = "새 비밀번호는 영문과 숫자를 포함하여 8~72자로 입력해 주세요."
    )
    private String newPassword = "";

    private String newPasswordConfirm = "";

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
    @Size(max = 100, message = "이메일은 100자 이내로 입력해 주세요.")
    private String empEmail;

    @NotBlank(message = "휴대전화 번호를 입력해 주세요.")
    @Pattern(
        regexp = "^01[0-9]{8,9}$",
        message = "휴대전화 번호는 숫자만 입력해 주세요."
    )
    private String empPhone;

    private String phoneVerificationToken = "";

    private String postcode;

    private String address;

    @Size(max = 100, message = "상세 주소는 100자 이내로 입력해 주세요.")
    private String addressDetail = "";

    public static MemberProfileUpdateRequest from(Employee employee) {
        MemberProfileUpdateRequest request = new MemberProfileUpdateRequest();
        request.empEmail = employee.getEmpEmail();
        request.empPhone = employee.getEmpPhone();

        String storedAddress = employee.getEmpAddress() == null
                ? "" : employee.getEmpAddress().trim();
        Matcher matcher = java.util.regex.Pattern
                .compile("^\\(([^)]+)\\)\\s*(.*)$")
                .matcher(storedAddress);
        if (matcher.matches()) {
            request.postcode = matcher.group(1);
            request.address = matcher.group(2);
        } else {
            request.postcode = "";
            request.address = storedAddress;
        }
        return request;
    }

    public boolean passwordChangeRequested() {
        return newPassword != null && !newPassword.isBlank();
    }

    public boolean passwordMatches() {
        return !passwordChangeRequested()
                || newPassword.equals(newPasswordConfirm);
    }

    public String fullAddress() {
        if (address == null || address.isBlank()) return "";
        String detail = addressDetail == null ? "" : addressDetail.trim();
        return String.format("(%s) %s%s",
                postcode == null ? "" : postcode.trim(),
                address.trim(),
                detail.isEmpty() ? "" : " " + detail).trim();
    }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getNewPasswordConfirm() { return newPasswordConfirm; }
    public void setNewPasswordConfirm(String newPasswordConfirm) { this.newPasswordConfirm = newPasswordConfirm; }
    public String getEmpEmail() { return empEmail; }
    public void setEmpEmail(String empEmail) { this.empEmail = empEmail; }
    public String getEmpPhone() { return empPhone; }
    public void setEmpPhone(String empPhone) { this.empPhone = empPhone; }
    public String getPhoneVerificationToken() { return phoneVerificationToken; }
    public void setPhoneVerificationToken(String phoneVerificationToken) { this.phoneVerificationToken = phoneVerificationToken; }
    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getAddressDetail() { return addressDetail; }
    public void setAddressDetail(String addressDetail) { this.addressDetail = addressDetail; }
}
