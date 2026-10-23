package com.transit.SGComplaint.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PasswordRecoveryVerifyRequest(
        @NotNull(message = "회원정보를 입력해 주세요.")
        @Valid PasswordRecoveryIdentityRequest identity,

        @NotBlank(message = "인증번호를 입력해 주세요.")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증번호 6자리를 입력해 주세요.")
        String code
) {
}
