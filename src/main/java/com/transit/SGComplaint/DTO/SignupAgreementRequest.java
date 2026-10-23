package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.AssertTrue;

public class SignupAgreementRequest {

    @AssertTrue(message = "이용약관에 동의해 주세요.")
    private boolean termsAgreed;

    @AssertTrue(message = "개인정보 수집 및 이용에 동의해 주세요.")
    private boolean privacyAgreed;

    @AssertTrue(message = "만 14세 이상 여부를 확인해 주세요.")
    private boolean ageConfirmed;

    public boolean isTermsAgreed() { return termsAgreed; }
    public void setTermsAgreed(boolean termsAgreed) { this.termsAgreed = termsAgreed; }
    public boolean isPrivacyAgreed() { return privacyAgreed; }
    public void setPrivacyAgreed(boolean privacyAgreed) { this.privacyAgreed = privacyAgreed; }
    public boolean isAgeConfirmed() { return ageConfirmed; }
    public void setAgeConfirmed(boolean ageConfirmed) { this.ageConfirmed = ageConfirmed; }
}
