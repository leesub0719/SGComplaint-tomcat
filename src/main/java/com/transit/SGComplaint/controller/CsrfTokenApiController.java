package com.transit.SGComplaint.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 분리된 JavaScript에서 REST 요청을 보낼 때 사용할 CSRF 토큰 발급 엔드포인트.
 *
 * <p>Thymeleaf 폼은 숨겨진 필드로 토큰을 받지만, 분리된 JavaScript의 REST 요청은
 * 이 엔드포인트에서 헤더 이름과 토큰을 받아 변경 요청 헤더에 붙인다.</p>
 *
 * <p>대안으로 {@code CookieCsrfTokenRepository.withHttpOnlyFalse()}를 쓰면
 * XSRF-TOKEN 쿠키로 받을 수 있지만, 그 방식은 앱 전역 설정을 바꾸므로
 * 기존 폼 화면 영향이 없는 이 방식을 택했다.</p>
 */
@RestController
public class CsrfTokenApiController {

    @GetMapping("/api/csrf")
    public Map<String, String> csrfToken(CsrfToken token) {
        return Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken());
    }
}
