package com.transit.SGComplaint.DTO;

import java.util.List;

public record AccountRecoveryResponse(
        boolean success,
        String message,
        List<String> employeeIds,
        String verificationToken
) {
    public static AccountRecoveryResponse success(String message) {
        return new AccountRecoveryResponse(true, message, null, null);
    }

    public static AccountRecoveryResponse found(List<String> employeeIds) {
        return new AccountRecoveryResponse(
                true, "가입된 아이디를 찾았습니다.", employeeIds, null);
    }

    public static AccountRecoveryResponse verified(String token) {
        return new AccountRecoveryResponse(
                true, "휴대전화 인증이 완료되었습니다.", null, token);
    }

    public static AccountRecoveryResponse failure(String message) {
        return new AccountRecoveryResponse(false, message, null, null);
    }
}
