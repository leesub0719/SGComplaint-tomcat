package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class PhoneVerification {
    private Long verificationNo;
    private String challengeScope;
    private String phone;
    private String codeHash;
    private String verificationTokenHash;
    private int failedAttempts;
    private LocalDateTime requestedAt;
    private LocalDateTime codeExpiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime usedAt;

    protected PhoneVerification() {
    }

    public static PhoneVerification create(
            String phone,
            String challengeScope,
            String codeHash,
            LocalDateTime codeExpiresAt) {

        PhoneVerification verification = new PhoneVerification();
        verification.phone = phone;
        verification.challengeScope = challengeScope;
        verification.codeHash = codeHash;
        verification.failedAttempts = 0;
        verification.requestedAt = now();
        verification.codeExpiresAt = codeExpiresAt;
        return verification;
    }

    public boolean isCodeExpired(LocalDateTime currentTime) {
        return !currentTime.isBefore(codeExpiresAt);
    }

    public boolean hasReachedAttemptLimit(int maxAttempts) {
        return failedAttempts >= maxAttempts;
    }

    public void recordFailedAttempt() { failedAttempts += 1; }

    public void markVerified(String tokenHash, LocalDateTime tokenExpiresAt) {
        this.verificationTokenHash = tokenHash;
        this.verifiedAt = now();
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public boolean isTokenExpired(LocalDateTime currentTime) {
        return tokenExpiresAt == null || !currentTime.isBefore(tokenExpiresAt);
    }

    public boolean isVerified() { return verifiedAt != null; }
    public boolean isAlreadyUsed() { return usedAt != null; }
    public void markUsed() { usedAt = now(); }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public String getCodeHash() { return codeHash; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
}
