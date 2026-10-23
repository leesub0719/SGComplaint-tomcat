package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.SignupAgreementEvidence;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 회원가입 1단계(약관 동의) 결과를 세션에 보관한다.
 *
 * <p>Thymeleaf 화면과 회원가입 REST API가 같은 동의 증적을 공유하도록
 * 컴포넌트로 분리했다.
 * {@link ProfileVerificationSession}과 같은 패턴이다.</p>
 */
@Component
public class SignupAgreementSession {

    private static final String ATTRIBUTE = "signupAgreementEvidence";
    public static final String TERMS_VERSION = "2026-09-04";
    public static final String PRIVACY_VERSION = "2026-09-07";
    private static final Duration VALID_DURATION = Duration.ofMinutes(30);

    /**
     * 유효한 동의 증적을 돌려준다. 버전이 다르거나 30분이 지났으면
     * 세션에서 제거하고 null을 돌려준다.
     */
    public SignupAgreementEvidence getValidAgreement(HttpSession session) {
        Object value = session.getAttribute(ATTRIBUTE);
        if (!(value instanceof SignupAgreementEvidence evidence)) {
            return null;
        }

        boolean correctVersion = TERMS_VERSION.equals(evidence.termsVersion())
                && PRIVACY_VERSION.equals(evidence.privacyVersion());
        boolean expired = evidence.agreedAt() == null
                || evidence.agreedAt().plus(VALID_DURATION).isBefore(LocalDateTime.now());

        if (!correctVersion || expired) {
            session.removeAttribute(ATTRIBUTE);
            return null;
        }
        return evidence;
    }

    /** 현재 버전 기준으로 동의 증적을 기록한다. */
    public void markAgreed(HttpSession session) {
        session.setAttribute(
                ATTRIBUTE,
                new SignupAgreementEvidence(
                        TERMS_VERSION,
                        PRIVACY_VERSION,
                        LocalDateTime.now()));
    }

    /** 가입 완료 후 증적을 비운다. */
    public void clear(HttpSession session) {
        session.removeAttribute(ATTRIBUTE);
    }
}
