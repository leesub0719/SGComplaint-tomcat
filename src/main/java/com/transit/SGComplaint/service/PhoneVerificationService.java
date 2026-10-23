package com.transit.SGComplaint.service;

import com.transit.SGComplaint.config.SmsProperties;
import com.transit.SGComplaint.domain.PhoneVerification;
import com.transit.SGComplaint.repository.PhoneVerificationRepository;
import com.transit.SGComplaint.sms.SmsSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PhoneVerificationService {

    private final PhoneVerificationRepository repository;
    private final SmsSender smsSender;
    private final PasswordEncoder passwordEncoder;
    private final SmsProperties properties;
    private final AbuseLimitService limits;
    @Value("${sms.verification.phone-hour-limit:5}") private int phoneHourLimit = 5;
    @Value("${sms.verification.phone-day-limit:10}") private int phoneDayLimit = 10;
    @Value("${sms.verification.global-day-limit:100}") private int globalDayLimit = 100;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneVerificationService(
            PhoneVerificationRepository repository,
            SmsSender smsSender,
            PasswordEncoder passwordEncoder,
            SmsProperties properties, AbuseLimitService limits) {
        this.repository = repository;
        this.smsSender = smsSender;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.limits = limits;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void requestCode(String rawPhone, String purpose) {
        String phone = PhoneNumberUtils.normalize(rawPhone);
        LocalDateTime now = now();
        long cooldown = properties.getVerification().getResendCooldownSeconds();

        String scope = scope(purpose);
        if (!limits.allow("sms-cooldown", phone, (int) cooldown, 1)
                || !limits.allow("sms-phone-hour", phone, 3600, phoneHourLimit)
                || !limits.allow("sms-phone-day", phone, 86400, phoneDayLimit)
                || !limits.allow("sms-global-day", "all", 86400, globalDayLimit)) {
            throw new PhoneVerificationException("문자 발송 제한에 도달했습니다. 잠시 후 다시 시도해 주세요.");
        }
        repository.findTopByPhoneOrderByRequestedAtDesc(phone, scope)
                .ifPresent(latest -> {
                    long passed = Duration.between(
                            latest.getRequestedAt(), now).getSeconds();
                    if (passed < cooldown) {
                        throw new PhoneVerificationException(
                                (cooldown - passed)
                                + "초 후에 인증번호를 다시 요청할 수 있습니다.");
                    }
                });

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);
        LocalDateTime expiresAt = now.plusSeconds(
                properties.getVerification().getCodeExpirationSeconds());

        PhoneVerification verification = PhoneVerification.create(
                phone, scope, codeHash, expiresAt);
        repository.invalidateScope(phone, scope);
        repository.saveAndFlush(verification);

        smsSender.sendVerificationCode(phone, code);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = PhoneVerificationException.class)
    public String verifyCode(String rawPhone, String code, String purpose) {
        String phone = PhoneNumberUtils.normalize(rawPhone);
        String normalizedCode = code == null ? "" : code.trim();

        if (!normalizedCode.matches("^[0-9]{6}$")) {
            throw new PhoneVerificationException(
                    "인증번호 6자리를 입력해 주세요.");
        }

        PhoneVerification verification = repository
                .findTopByPhoneOrderByRequestedAtDesc(phone, scope(purpose))
                .orElseThrow(() -> new PhoneVerificationException(
                        "먼저 인증번호를 요청해 주세요."));

        LocalDateTime now = now();
        int maxAttempts = properties.getVerification().getMaxFailedAttempts();

        if (verification.isAlreadyUsed() || verification.isVerified()) {
            throw new PhoneVerificationException("이미 처리된 인증번호입니다. 새 인증번호를 요청해 주세요.");
        }
        if (verification.isCodeExpired(now)) {
            throw new PhoneVerificationException(
                    "인증번호가 만료되었습니다. 다시 요청해 주세요.");
        }

        if (verification.hasReachedAttemptLimit(maxAttempts)) {
            throw new PhoneVerificationException(
                    "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요.");
        }

        if (!passwordEncoder.matches(normalizedCode, verification.getCodeHash())) {
            verification.recordFailedAttempt();
            repository.save(verification);
            throw new PhoneVerificationException(
                    "인증번호가 일치하지 않습니다.");
        }

        String rawToken = createSecureToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime tokenExpiresAt = now.plusSeconds(
                properties.getVerification().getTokenExpirationSeconds());

        verification.markVerified(tokenHash, tokenExpiresAt);
        repository.save(verification);
        return rawToken;
    }

    @Transactional
    public void consumeVerification(String rawPhone, String rawToken, String purpose) {
        String phone = PhoneNumberUtils.normalize(rawPhone);

        if (rawToken == null || rawToken.isBlank()) {
            throw new PhoneVerificationException(
                    "휴대전화 인증을 완료해 주세요.");
        }

        PhoneVerification verification = repository
                .findByPhoneAndVerificationTokenHashAndUsedAtIsNull(
                        phone, hashToken(rawToken), scope(purpose))
                .orElseThrow(() -> new PhoneVerificationException(
                        "유효한 휴대전화 인증정보가 없습니다."));

        if (verification.isAlreadyUsed()
                || verification.isTokenExpired(now())) {
            throw new PhoneVerificationException(
                    "휴대전화 인증이 만료되었습니다. 다시 인증해 주세요.");
        }

        verification.markUsed();
        repository.save(verification);
    }

    private String scope(String purpose) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            throw new PhoneVerificationException("인증 세션을 확인할 수 없습니다.");
        }
        return hashToken(attrs.getRequest().getSession().getId() + ":" + purpose);
    }

    private String createSecureToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
