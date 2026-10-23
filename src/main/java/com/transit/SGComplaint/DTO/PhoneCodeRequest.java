package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneCodeRequest(
        @NotBlank(message = "휴대전화 번호를 입력해 주세요.")
        @Pattern(
            regexp = "^01[0-9]{8,9}$",
            message = "올바른 휴대전화 번호를 입력해 주세요."
        )
        String phone
) {
}
