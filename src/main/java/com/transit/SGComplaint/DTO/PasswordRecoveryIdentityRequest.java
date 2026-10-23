package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordRecoveryIdentityRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "올바른 아이디를 입력해 주세요.")
        String empId,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 50, message = "이름은 50자 이내로 입력해 주세요.")
        String empName,

        @NotBlank(message = "휴대전화 번호를 입력해 주세요.")
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대전화 번호를 입력해 주세요.")
        String phone
) {
}
