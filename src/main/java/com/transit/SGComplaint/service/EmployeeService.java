package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.EmployeeSignupRequest;
import com.transit.SGComplaint.DTO.MemberProfileUpdateRequest;
import com.transit.SGComplaint.DTO.SignupAgreementEvidence;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import com.transit.SGComplaint.mapper.SignupConsentMapper;
import com.transit.SGComplaint.mapper.WithdrawalHistoryMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeMapper employeeRepository;
    private final SignupConsentMapper signupConsentMapper;
    private final PhoneVerificationService phoneVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final WithdrawalHistoryMapper withdrawalHistoryMapper;
    private final PersistentTokenRepository persistentTokenRepository;
    private final int withdrawalHistoryDays;

    public EmployeeService(
            EmployeeMapper employeeRepository,
            SignupConsentMapper signupConsentMapper,
            PhoneVerificationService phoneVerificationService,
            PasswordEncoder passwordEncoder,
            WithdrawalHistoryMapper withdrawalHistoryMapper,
            PersistentTokenRepository persistentTokenRepository,
            @Value("${app.retention.withdrawn-personal-days:30}") int withdrawalHistoryDays) {
        this.employeeRepository = employeeRepository;
        this.signupConsentMapper = signupConsentMapper;
        this.phoneVerificationService = phoneVerificationService;
        this.passwordEncoder = passwordEncoder;
        this.withdrawalHistoryMapper = withdrawalHistoryMapper;
        this.persistentTokenRepository = persistentTokenRepository;
        this.withdrawalHistoryDays = Math.max(1, withdrawalHistoryDays);
    }

    public boolean isDuplicateId(String empId) {
        if (empId == null) {
            return false;
        }
        return employeeRepository.existsByEmpIdAndEmpStatus(
                empId.trim().toLowerCase(), "Y");
    }

    public boolean isEmpIdAvailable(String empId) {
        if (empId == null || !empId.matches("^[a-z0-9]{4,20}$")) {
            return false;
        }
        return !isDuplicateId(empId);
    }

    public String getActiveEmployeeName(String empId) {
        return getRequiredActiveEmployee(empId).getEmpName();
    }

    public Employee getRequiredActiveEmployee(String empId) {
        return employeeRepository.findByEmpIdAndEmpStatus(empId, "Y")
                .orElseThrow(() -> new IllegalStateException(
                        "로그인 회원 정보를 찾을 수 없습니다."
                ));
    }

    public boolean isActiveAdministrator(String empId) {
        return employeeRepository.findByEmpIdAndEmpStatus(empId, "Y")
                .map(Employee::hasAdminRole)
                .orElse(false);
    }

    public boolean matchesCurrentPassword(String empId, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        return passwordEncoder.matches(
                rawPassword,
                getRequiredActiveEmployee(empId).getEmpPassword());
    }

    public MemberProfileUpdateRequest getProfileUpdateForm(String empId) {
        return MemberProfileUpdateRequest.from(getRequiredActiveEmployee(empId));
    }

    @Transactional
    public void updateProfile(String empId, MemberProfileUpdateRequest request) {
        if (!request.passwordMatches()) {
            throw new IllegalArgumentException(
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        Employee employee = getRequiredActiveEmployee(empId);
        String phone = PhoneNumberUtils.normalize(request.getEmpPhone());
        if (!employee.getEmpPhone().equals(phone)) {
            phoneVerificationService.consumeVerification(
                    phone, request.getPhoneVerificationToken(), "PROFILE:" + employee.getEmpNo());
        }

        String encodedPassword = request.passwordChangeRequested()
                ? passwordEncoder.encode(request.getNewPassword())
                : null;
        employee.updateProfile(
                encodedPassword,
                request.getEmpEmail().trim(),
                phone,
                request.fullAddress());
        employeeRepository.updateEmployee(employee);
        if (encodedPassword != null) {
            persistentTokenRepository.removeUserTokens(empId);
        }
    }

    @Transactional
    public void withdraw(String empId) {
        Employee employee = getRequiredActiveEmployee(empId);
        if (employee.isMaster()) {
            throw new IllegalStateException(
                    "마스터 계정은 사용자 페이지에서 탈퇴할 수 없습니다.");
        }
        String originalId = employee.getEmpId();
        String withdrawnId = createWithdrawnId();
        withdrawalHistoryMapper.insert(employee.getEmpNo(), originalId, withdrawnId,
                "WITHDRAWAL", originalId,
                LocalDateTime.now().plusDays(withdrawalHistoryDays));
        employee.withdraw(withdrawnId);
        employeeRepository.updateEmployee(employee);
        persistentTokenRepository.removeUserTokens(originalId);
    }

    @Transactional
    public Long signupUser(
            EmployeeSignupRequest request,
            SignupAgreementEvidence agreement) {
        if (agreement == null) {
            throw new IllegalArgumentException(
                    "필수 약관 및 개인정보 동의가 필요합니다.");
        }
        return signup(request, false, agreement);
    }

    @Transactional
    public Long signupAdmin(EmployeeSignupRequest request) {
        return signup(request, true, null);
    }

    private Long signup(
            EmployeeSignupRequest request,
            boolean admin,
            SignupAgreementEvidence agreement) {
        if (!request.passwordMatches()) {
            throw new IllegalArgumentException(
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        String empId = request.getEmpId().trim().toLowerCase();
        String phone = PhoneNumberUtils.normalize(request.getEmpPhone());

        if (employeeRepository.existsByEmpIdAndEmpStatus(empId, "Y")) {
            throw new DuplicateEmployeeIdException(
                    "이미 사용 중인 아이디입니다.");
        }

        // 기존 탈퇴 회원이 같은 아이디를 보유한 경우 UNIQUE 제약을 피하도록
        // 탈퇴 행의 로그인 아이디만 내부 식별값으로 변경하고 나머지 기록은 보존합니다.
        employeeRepository.findByEmpIdAndEmpStatus(empId, "N").ifPresent(withdrawn -> {
            String replacementId = createWithdrawnId();
            withdrawalHistoryMapper.insert(withdrawn.getEmpNo(), withdrawn.getEmpId(), replacementId,
                    "ID_RELEASE", "SYSTEM",
                    LocalDateTime.now().plusDays(withdrawalHistoryDays));
            employeeRepository.releaseWithdrawnEmployeeId(empId, replacementId);
        });


        String encodedPassword = passwordEncoder.encode(
                request.getEmpPassword());

        Employee employee = admin
                ? Employee.createAdmin(
                    empId, encodedPassword, request.getEmpName(),
                    request.getEmpEmail(), phone, "")
                : Employee.createUser(
                    empId, encodedPassword, request.getEmpName(),
                    request.getEmpEmail(), phone, "");

        try {
            Long empNo = employeeRepository.saveAndFlush(employee).getEmpNo();
            if (!admin) {
                signupConsentMapper.insertSignupConsent(
                        empNo,
                        agreement.termsVersion(),
                        agreement.privacyVersion(),
                        agreement.agreedAt());
            }
            return empNo;
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmployeeIdException(
                    "이미 사용 중인 아이디입니다.");
        }
    }

    private String createWithdrawnId() {
        return "w" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 19);
    }
}
