package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.EmployeeSignupRequest;
import com.transit.SGComplaint.DTO.SignupAgreementEvidence;
import com.transit.SGComplaint.DTO.SignupAgreementRequest;
import com.transit.SGComplaint.DTO.SignupApiResponse;
import com.transit.SGComplaint.service.EmployeeService;
import com.transit.SGComplaint.service.SignupAgreementSession;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 회원가입 화면의 JSON API.
 *
 * <p>계정 찾기/비밀번호 재설정은 이미 {@link AccountRecoveryController}가 REST로
 * 제공하므로, JavaScript 또는 외부 클라이언트는 이 회원가입 API를 사용할 수 있다.</p>
 *
 * <p>아이디 중복확인은 기존 {@code GET /api/members/check-id}를 그대로 쓴다.</p>
 */
@RestController
@RequestMapping("/api/signup")
public class SignupApiController {

    private final EmployeeService employeeService;
    private final SignupAgreementSession agreementSession;

    public SignupApiController(
            EmployeeService employeeService,
            SignupAgreementSession agreementSession) {
        this.employeeService = employeeService;
        this.agreementSession = agreementSession;
    }

    /** 진입 시 호출: 약관 동의를 이미 마쳤는지와 현재 약관 버전. */
    @GetMapping("/state")
    public Map<String, Object> state(HttpSession session) {
        return Map.of(
                "agreed", agreementSession.getValidAgreement(session) != null,
                "termsVersion", SignupAgreementSession.TERMS_VERSION,
                "privacyVersion", SignupAgreementSession.PRIVACY_VERSION);
    }

    /** 1단계: 약관 동의. 세 항목 모두 true여야 통과한다. */
    @PostMapping("/terms")
    public SignupApiResponse acceptTerms(
            @Valid @RequestBody SignupAgreementRequest request,
            HttpSession session) {
        agreementSession.markAgreed(session);
        return SignupApiResponse.ok("약관에 동의했습니다.");
    }

    /** 2단계: 회원정보 저장. */
    @PostMapping
    public ResponseEntity<SignupApiResponse> signup(
            @Valid @RequestBody EmployeeSignupRequest request,
            HttpSession session) {
        SignupAgreementEvidence agreement = agreementSession.getValidAgreement(session);
        if (agreement == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    SignupApiResponse.fail("약관 동의가 만료되었습니다. 처음부터 다시 진행해 주세요."));
        }
        if (!request.passwordMatches()) {
            return ResponseEntity.badRequest().body(
                    SignupApiResponse.fieldError(
                            "passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다."));
        }

        Long empNo = employeeService.signupUser(request, agreement);
        agreementSession.clear(session);
        return ResponseEntity.ok(SignupApiResponse.created(empNo));
    }
}
