package com.transit.SGComplaint.service;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    public static String normalize(String phone) {
        String normalized = digitsOnly(phone);
        if (!normalized.matches("^01[0-9]{8,9}$")) {
            throw new PhoneVerificationException(
                    "올바른 휴대전화 번호를 입력해 주세요.");
        }
        return normalized;
    }

    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }
}
