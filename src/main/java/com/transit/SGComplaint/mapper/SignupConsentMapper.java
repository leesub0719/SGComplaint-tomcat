package com.transit.SGComplaint.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SignupConsentMapper {

    int insertSignupConsent(
            @Param("empNo") Long empNo,
            @Param("termsVersion") String termsVersion,
            @Param("privacyVersion") String privacyVersion,
            @Param("agreedAt") LocalDateTime agreedAt);
}
