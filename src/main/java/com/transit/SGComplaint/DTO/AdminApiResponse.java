package com.transit.SGComplaint.DTO;

/**
 * 관리자 REST API 공통 응답.
 *
 * <p>기존 관리자 화면은 처리 결과를 {@code RedirectAttributes}의
 * successMessage / errorMessage 플래시 속성으로 전달하고 목록으로 리다이렉트했다.
 * JavaScript 클라이언트는 리다이렉트 없이 이 응답의 메시지를 화면에 표시하고
 * 필요한 목록만 다시 조회한다.</p>
 */
public record AdminApiResponse(boolean success, String message) {

    public static AdminApiResponse ok(String message) {
        return new AdminApiResponse(true, message);
    }

    public static AdminApiResponse fail(String message) {
        return new AdminApiResponse(false, message);
    }
}
