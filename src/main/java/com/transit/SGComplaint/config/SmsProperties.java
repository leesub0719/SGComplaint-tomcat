package com.transit.SGComplaint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private final Solapi solapi = new Solapi();
    private final Verification verification = new Verification();

    public Solapi getSolapi() { return solapi; }
    public Verification getVerification() { return verification; }

    public static class Solapi {
        private String apiKey;
        private String apiSecret;
        private String sender;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
    }

    public static class Verification {
        private long codeExpirationSeconds = 180;
        private long resendCooldownSeconds = 60;
        private long tokenExpirationSeconds = 600;
        private int maxFailedAttempts = 5;

        public long getCodeExpirationSeconds() { return codeExpirationSeconds; }
        public void setCodeExpirationSeconds(long value) { this.codeExpirationSeconds = value; }
        public long getResendCooldownSeconds() { return resendCooldownSeconds; }
        public void setResendCooldownSeconds(long value) { this.resendCooldownSeconds = value; }
        public long getTokenExpirationSeconds() { return tokenExpirationSeconds; }
        public void setTokenExpirationSeconds(long value) { this.tokenExpirationSeconds = value; }
        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int value) { this.maxFailedAttempts = value; }
    }
}
