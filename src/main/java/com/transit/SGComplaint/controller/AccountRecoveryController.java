package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.AccountRecoveryResponse;
import com.transit.SGComplaint.DTO.FindIdRequest;
import com.transit.SGComplaint.DTO.FindIdVerifyRequest;
import com.transit.SGComplaint.DTO.PasswordRecoveryIdentityRequest;
import com.transit.SGComplaint.DTO.PasswordRecoveryVerifyRequest;
import com.transit.SGComplaint.DTO.PasswordResetRequest;
import com.transit.SGComplaint.service.AccountRecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account-recovery")
public class AccountRecoveryController {

    private final AccountRecoveryService accountRecoveryService;

    public AccountRecoveryController(
            AccountRecoveryService accountRecoveryService) {
        this.accountRecoveryService = accountRecoveryService;
    }

    @PostMapping("/find-id/request")
    public AccountRecoveryResponse requestFindIdCode(
            @Valid @RequestBody FindIdRequest request) {
        accountRecoveryService.requestFindIdCode(request.phone());
        return AccountRecoveryResponse.success(
                "인증번호를 문자로 발송했습니다.");
    }

    @PostMapping("/find-id/verify")
    public AccountRecoveryResponse verifyAndFindId(
            @Valid @RequestBody FindIdVerifyRequest request) {
        return AccountRecoveryResponse.found(
                accountRecoveryService.verifyAndFindIds(
                        request.phone(), request.code()));
    }

    @PostMapping("/password/request")
    public AccountRecoveryResponse requestPasswordCode(
            @Valid @RequestBody PasswordRecoveryIdentityRequest request) {
        accountRecoveryService.requestPasswordCode(request);
        return AccountRecoveryResponse.success(
                "인증번호를 문자로 발송했습니다.");
    }

    @PostMapping("/password/check-id")
    public AccountRecoveryResponse checkPasswordRecoveryId(
            @Valid @RequestBody EmployeeIdRequest request) {
        accountRecoveryService.checkPasswordRecoveryId(request.empId());
        return AccountRecoveryResponse.success(
                "아이디가 확인되었습니다.");
    }

    @PostMapping("/password/verify")
    public AccountRecoveryResponse verifyPasswordCode(
            @Valid @RequestBody PasswordRecoveryVerifyRequest request) {
        return AccountRecoveryResponse.verified(
                accountRecoveryService.verifyPasswordCode(
                        request.identity(), request.code()));
    }

    @PostMapping("/password/reset")
    public AccountRecoveryResponse resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        accountRecoveryService.resetPassword(
                request.identity(),
                request.verificationToken(),
                request.newPassword(),
                request.newPasswordConfirm());
        return AccountRecoveryResponse.success(
                "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
    }


    public record EmployeeIdRequest(
            @NotBlank(message = "아이디를 입력해 주세요.")
            @Pattern(
                regexp = "^[a-z0-9]{4,20}$",
                message = "올바른 아이디를 입력해 주세요."
            )
            String empId
    ) {
    }
}
