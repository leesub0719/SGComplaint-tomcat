package com.transit.SGComplaint.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotNull(message = "회원정보를 입력해 주세요.")
        @Valid PasswordRecoveryIdentityRequest identity,

        @NotBlank(message = "휴대전화 인증을 완료해 주세요.")
        String verificationToken,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 72, message = "비밀번호는 8~72자로 입력해 주세요.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$",
            message = "비밀번호에는 영문과 숫자가 포함되어야 합니다."
        )
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인을 입력해 주세요.")
        String newPasswordConfirm
) {
}
