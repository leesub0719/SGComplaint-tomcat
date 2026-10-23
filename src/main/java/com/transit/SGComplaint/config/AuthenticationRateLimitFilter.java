package com.transit.SGComplaint.config;

import com.transit.SGComplaint.service.AbuseLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private final AbuseLimitService limits;
    public AuthenticationRateLimitFilter(AbuseLimitService limits) { this.limits = limits; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        boolean api = path.startsWith("/api/account-recovery/")
                || path.startsWith("/api/phone-verifications/")
                || path.equals("/api/members/check-id");
        boolean post = "POST".equals(request.getMethod());
        if (api || (post && (path.equals("/login") || path.equals("/signup")
                || path.equals("/signup/terms")))) {
            // Forwarded 헤더를 직접 신뢰하지 않습니다. 기본은 실제 연결 IP입니다.
            String ip = request.getRemoteAddr();
            boolean allowed = limits.allow("auth-ip", ip, 60, 30);
            if (allowed && post && path.equals("/login")) {
                String id = request.getParameter("empId");
                allowed = limits.allow("login-account", id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT), 900, 20);
            }
            if (allowed && post && path.endsWith("/request")) {
                allowed = limits.allow("sms-ip", ip, 3600, 20);
            }
            if (allowed && post && path.equals("/signup")) {
                allowed = limits.allow("signup-ip", ip, 3600, 10);
            }
            if (!allowed) {
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"요청 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
