package com.transit.SGComplaint.DTO;

public record PhoneVerificationResponse(
        boolean success,
        String message,
        String verificationToken
) {
    public static PhoneVerificationResponse success(String message) {
        return new PhoneVerificationResponse(true, message, null);
    }

    public static PhoneVerificationResponse verified(
            String message, String verificationToken) {
        return new PhoneVerificationResponse(
                true, message, verificationToken);
    }

    public static PhoneVerificationResponse failure(String message) {
        return new PhoneVerificationResponse(false, message, null);
    }
}
