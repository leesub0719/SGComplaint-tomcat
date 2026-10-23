package com.transit.SGComplaint.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * 마이페이지 정보수정 진입 전 "비밀번호 재확인" 통과 여부를 세션에 보관한다.
 *
 * <p>Thymeleaf 화면 요청과 마이페이지 REST API가 동일한 세션 게이트를
 * 공유하도록 별도 컴포넌트로 분리했다.</p>
 */
@Component
public class ProfileVerificationSession {

    private static final String ATTRIBUTE = "mypageProfileVerifiedAt";
    private static final long VALID_MILLIS = 10 * 60 * 1000L;

    /** 재확인이 유효한 상태인지. 만료됐으면 세션 속성을 정리하고 false를 돌려준다. */
    public boolean isVerified(HttpSession session) {
        Object verifiedAt = session.getAttribute(ATTRIBUTE);
        if (!(verifiedAt instanceof Long timestamp)) {
            return false;
        }
        if (System.currentTimeMillis() - timestamp > VALID_MILLIS) {
            session.removeAttribute(ATTRIBUTE);
            return false;
        }
        return true;
    }

    /** 비밀번호 재확인 성공 시각을 기록한다. */
    public void markVerified(HttpSession session) {
        session.setAttribute(ATTRIBUTE, System.currentTimeMillis());
    }

    /** 남은 유효 시간(ms). 만료·미인증이면 0이며 화면 카운트다운에 사용한다. */
    public long remainingMillis(HttpSession session) {
        if (!isVerified(session)) {
            return 0L;
        }
        long timestamp = (Long) session.getAttribute(ATTRIBUTE);
        return Math.max(0L, VALID_MILLIS - (System.currentTimeMillis() - timestamp));
    }

    public void clear(HttpSession session) {
        session.removeAttribute(ATTRIBUTE);
    }
}
