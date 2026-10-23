package com.transit.SGComplaint.DTO;

import java.io.Serializable;
import java.time.LocalDateTime;

public record SignupAgreementEvidence(
        String termsVersion,
        String privacyVersion,
        LocalDateTime agreedAt) implements Serializable {
}
