package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class JobApplicationRequest {
    @NotBlank(message = "지원자 성명을 입력해 주세요.")
    @Size(max = 50, message = "성명은 50자 이내로 입력해 주세요.")
    private String applicantName;

    @NotBlank(message = "연락처를 입력해 주세요.")
    @Pattern(regexp = "^[0-9+()\\- ]{8,30}$", message = "연락처 형식을 확인해 주세요.")
    private String phone;

    @Email(message = "이메일 형식을 확인해 주세요.")
    @Size(max = 100, message = "이메일은 100자 이내로 입력해 주세요.")
    private String email;

    @NotBlank(message = "지원 구분을 선택해 주세요.")
    @Pattern(regexp = "NEW|EXPERIENCED", message = "지원 구분이 올바르지 않습니다.")
    private String applicantType;

    @Size(max = 2000, message = "지원 메모는 2,000자 이내로 입력해 주세요.")
    private String memo;

    @AssertTrue(message = "개인정보 및 채용서류 수집·이용에 동의해 주세요.")
    private boolean consented;

    private MultipartFile resume;
    private MultipartFile driverLicense;
    private MultipartFile busDriverLicense;
    private MultipartFile aptitudeResult;
    private MultipartFile trainingCertificate;
    private MultipartFile drivingCareer;
    private MultipartFile careerCertificate;

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getApplicantType() { return applicantType; }
    public void setApplicantType(String applicantType) { this.applicantType = applicantType; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public boolean isConsented() { return consented; }
    public void setConsented(boolean consented) { this.consented = consented; }
    public MultipartFile getResume() { return resume; }
    public void setResume(MultipartFile resume) { this.resume = resume; }
    public MultipartFile getDriverLicense() { return driverLicense; }
    public void setDriverLicense(MultipartFile driverLicense) { this.driverLicense = driverLicense; }
    public MultipartFile getBusDriverLicense() { return busDriverLicense; }
    public void setBusDriverLicense(MultipartFile busDriverLicense) { this.busDriverLicense = busDriverLicense; }
    public MultipartFile getAptitudeResult() { return aptitudeResult; }
    public void setAptitudeResult(MultipartFile aptitudeResult) { this.aptitudeResult = aptitudeResult; }
    public MultipartFile getTrainingCertificate() { return trainingCertificate; }
    public void setTrainingCertificate(MultipartFile trainingCertificate) { this.trainingCertificate = trainingCertificate; }
    public MultipartFile getDrivingCareer() { return drivingCareer; }
    public void setDrivingCareer(MultipartFile drivingCareer) { this.drivingCareer = drivingCareer; }
    public MultipartFile getCareerCertificate() { return careerCertificate; }
    public void setCareerCertificate(MultipartFile careerCertificate) { this.careerCertificate = careerCertificate; }
}
