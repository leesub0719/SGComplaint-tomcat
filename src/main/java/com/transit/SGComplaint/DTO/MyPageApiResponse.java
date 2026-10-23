package com.transit.SGComplaint.DTO;

import java.util.Map;

/**
 * 마이페이지 REST API 공통 응답.
 *
 * <p>{@code fieldErrors}는 필드명 → 메시지 형태로, JavaScript 폼에서 각 입력 아래에
 * 그대로 표시할 수 있도록 구성한다.</p>
 */
public record MyPageApiResponse(
        boolean success,
        String message,
        Map<String, String> fieldErrors
) {

    public static MyPageApiResponse ok(String message) {
        return new MyPageApiResponse(true, message, Map.of());
    }

    public static MyPageApiResponse fail(String message) {
        return new MyPageApiResponse(false, message, Map.of());
    }

    public static MyPageApiResponse fail(String message, Map<String, String> fieldErrors) {
        return new MyPageApiResponse(false, message, fieldErrors);
    }

    public static MyPageApiResponse fieldError(String field, String message) {
        return new MyPageApiResponse(false, message, Map.of(field, message));
    }
}
