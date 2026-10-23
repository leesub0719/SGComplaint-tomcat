package com.transit.SGComplaint.repository;

import com.transit.SGComplaint.domain.PhoneVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;

@Mapper
public interface PhoneVerificationRepository {
    int invalidateScope(@Param("phone") String phone, @Param("scope") String scope);
    Optional<PhoneVerification> findTopByPhoneOrderByRequestedAtDesc(@Param("phone") String phone, @Param("scope") String scope);
    Optional<PhoneVerification> findByPhoneAndVerificationTokenHashAndUsedAtIsNull(@Param("phone") String phone, @Param("verificationTokenHash") String verificationTokenHash, @Param("scope") String scope);
    int insertVerification(PhoneVerification verification);
    int updateVerification(PhoneVerification verification);
    default PhoneVerification saveAndFlush(PhoneVerification verification) { insertVerification(verification); return verification; }
    default PhoneVerification save(PhoneVerification verification) { updateVerification(verification); return verification; }
}
