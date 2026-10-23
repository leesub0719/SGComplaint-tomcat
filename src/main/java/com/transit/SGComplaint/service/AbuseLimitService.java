package com.transit.SGComplaint.service;

import com.transit.SGComplaint.mapper.AbuseLimitMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AbuseLimitService {
    private final AbuseLimitMapper mapper;
    public AbuseLimitService(AbuseLimitMapper mapper) { this.mapper = mapper; }

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    @Transactional
    public void deleteExpiredCounters() { mapper.deleteExpired(); }

    // 별도 트랜잭션: SMS 실패나 계정 검증 실패에도 사용량은 되돌리지 않습니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean allow(String category, String identity, int seconds, int limit) {
        String key = category + ":" + hash(identity);
        mapper.ensureBucket(key);
        return mapper.take(key, seconds, limit) == 1;
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
