package com.transit.SGComplaint.DTO;

import java.util.Map;

/**
 * 회원가입 REST API 공통 응답.
 *
 * <p>{@link MyPageApiResponse}와 같은 구조에 가입 완료 시 회원번호({@code empNo})만
 * 추가했다. 두 응답 타입은 나중에 하나로 합쳐도 된다.</p>
 */
public record SignupApiResponse(
        boolean success,
        String message,
        Map<String, String> fieldErrors,
        Long empNo
) {

    public static SignupApiResponse ok(String message) {
        return new SignupApiResponse(true, message, Map.of(), null);
    }

    public static SignupApiResponse created(Long empNo) {
        return new SignupApiResponse(true, "회원가입이 완료되었습니다.", Map.of(), empNo);
    }

    public static SignupApiResponse fail(String message) {
        return new SignupApiResponse(false, message, Map.of(), null);
    }

    public static SignupApiResponse fail(String message, Map<String, String> fieldErrors) {
        return new SignupApiResponse(false, message, fieldErrors, null);
    }

    public static SignupApiResponse fieldError(String field, String message) {
        return new SignupApiResponse(false, message, Map.of(field, message), null);
    }
}
