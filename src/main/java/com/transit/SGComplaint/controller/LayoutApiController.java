package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.service.EmployeeService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JavaScript와 외부 연동 화면에서 사용할 수 있는 로그인 상태 API.
 *
 * <p>Thymeleaf 화면은 {@link PublicLayoutControllerAdvice}가 모든 요청에
 * {@code isLoggedIn} / {@code memberName} / {@code isAdmin}을 Model에 넣어주기 때문에
 * 헤더에서 {@code th:if}로 로그인/로그아웃 영역을 나눌 수 있었다.</p>
 *
 * <p>Model을 사용할 수 없는 비동기 요청에도 같은 판정 결과를 JSON으로
 * 제공한다.</p>
 *
 * <p>비로그인 상태에서도 헤더를 그려야 하므로 이 엔드포인트는 인증 없이 접근할 수
 * 있고, 로그인하지 않았으면 {@code loggedIn: false}만 돌려준다.</p>
 */
@RestController
public class LayoutApiController {

    private final EmployeeService employeeService;

    public LayoutApiController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/api/layout/me")
    public LayoutMember me(Authentication authentication) {
        boolean loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (!loggedIn) {
            return new LayoutMember(false, null, false);
        }

        boolean hasAdminAuthority = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_MASTER".equals(authority.getAuthority()));

        return new LayoutMember(
                true,
                employeeService.getActiveEmployeeName(authentication.getName()),
                hasAdminAuthority
                        && employeeService.isActiveAdministrator(authentication.getName()));
    }

    public record LayoutMember(boolean loggedIn, String memberName, boolean admin) {
    }
}
