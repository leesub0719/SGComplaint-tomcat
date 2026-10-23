package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.NotBlank;

public class MemberPasswordConfirmRequest {

    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    private String currentPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
