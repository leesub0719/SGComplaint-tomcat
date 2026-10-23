package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneCodeVerifyRequest(
        @NotBlank(message = "휴대전화 번호를 입력해 주세요.")
        @Pattern(
            regexp = "^01[0-9]{8,9}$",
            message = "올바른 휴대전화 번호를 입력해 주세요."
        )
        String phone,

        @NotBlank(message = "인증번호를 입력해 주세요.")
        @Pattern(
            regexp = "^[0-9]{6}$",
            message = "인증번호 6자리를 입력해 주세요."
        )
        String code
) {
}
