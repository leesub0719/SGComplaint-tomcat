package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.PhoneCodeRequest;
import com.transit.SGComplaint.DTO.PhoneCodeVerifyRequest;
import com.transit.SGComplaint.DTO.PhoneVerificationResponse;
import com.transit.SGComplaint.service.PhoneVerificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import com.transit.SGComplaint.service.EmployeeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/phone-verifications")
public class PhoneVerificationController {

    private final EmployeeService employees;
    private final PhoneVerificationService phoneVerificationService;

    public PhoneVerificationController(
            PhoneVerificationService phoneVerificationService, EmployeeService employees) {
        this.phoneVerificationService = phoneVerificationService;
        this.employees = employees;
    }

    @PostMapping("/request")
    public PhoneVerificationResponse requestCode(
            @Valid @RequestBody PhoneCodeRequest request, Principal principal) {
        phoneVerificationService.requestCode(request.phone(), "PROFILE:" + employees.getRequiredActiveEmployee(principal.getName()).getEmpNo());
        return PhoneVerificationResponse.success(
                "인증번호를 문자로 발송했습니다.");
    }

    @PostMapping("/verify")
    public PhoneVerificationResponse verifyCode(
            @Valid @RequestBody PhoneCodeVerifyRequest request, Principal principal) {
        String token = phoneVerificationService.verifyCode(
                request.phone(), request.code(), "PROFILE:" + employees.getRequiredActiveEmployee(principal.getName()).getEmpNo());
        return PhoneVerificationResponse.verified(
                "휴대전화 인증이 완료되었습니다.", token);
    }
}
