package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.PasswordRecoveryIdentityRequest;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountRecoveryService {

    private static final String ACTIVE = "Y";

    private final EmployeeMapper employeeRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final PersistentTokenRepository persistentTokenRepository;

    public AccountRecoveryService(
            EmployeeMapper employeeRepository,
            PhoneVerificationService phoneVerificationService,
            PasswordEncoder passwordEncoder,
            PersistentTokenRepository persistentTokenRepository) {
        this.employeeRepository = employeeRepository;
        this.phoneVerificationService = phoneVerificationService;
        this.passwordEncoder = passwordEncoder;
        this.persistentTokenRepository = persistentTokenRepository;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void requestFindIdCode(String rawPhone) {
        String phone = PhoneNumberUtils.normalize(rawPhone);
        if (findActiveEmployeesByPhone(phone).isEmpty()) {
            throw new AccountRecoveryException(
                    "입력하신 휴대전화 번호로 가입된 계정을 찾을 수 없습니다.");
        }
        phoneVerificationService.requestCode(phone, "FIND_ID");
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<String> verifyAndFindIds(String rawPhone, String code) {
        String phone = PhoneNumberUtils.normalize(rawPhone);
        List<String> ids = findActiveEmployeesByPhone(phone).stream()
                .map(Employee::getEmpId)
                .toList();
        if (ids.isEmpty()) {
            throw new AccountRecoveryException(
                    "입력하신 휴대전화 번호로 가입된 계정을 찾을 수 없습니다.");
        }

        String token = phoneVerificationService.verifyCode(phone, code, "FIND_ID");
        phoneVerificationService.consumeVerification(phone, token, "FIND_ID");
        return ids;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void requestPasswordCode(PasswordRecoveryIdentityRequest request) {
        Employee employee = findMatchingEmployee(request);
        phoneVerificationService.requestCode(employee.getEmpPhone(), "RESET:" + employee.getEmpNo());
    }

    public void checkPasswordRecoveryId(String rawEmpId) {
        String empId = rawEmpId == null ? "" : rawEmpId.trim().toLowerCase();
        employeeRepository.findByEmpIdAndEmpStatus(empId, ACTIVE)
                .orElseThrow(() -> new AccountRecoveryException(
                        "입력하신 아이디로 가입된 계정을 찾을 수 없습니다."));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String verifyPasswordCode(
            PasswordRecoveryIdentityRequest request, String code) {
        Employee employee = findMatchingEmployee(request);
        return phoneVerificationService.verifyCode(employee.getEmpPhone(), code, "RESET:" + employee.getEmpNo());
    }

    @Transactional
    public void resetPassword(
            PasswordRecoveryIdentityRequest request,
            String verificationToken,
            String newPassword,
            String newPasswordConfirm) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new AccountRecoveryException(
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        Employee employee = findMatchingEmployee(request);
        phoneVerificationService.consumeVerification(
                employee.getEmpPhone(), verificationToken, "RESET:" + employee.getEmpNo());
        employee.changePassword(passwordEncoder.encode(newPassword));
        employeeRepository.updateEmployee(employee);
        persistentTokenRepository.removeUserTokens(employee.getEmpId());
    }

    private List<Employee> findActiveEmployeesByPhone(String phone) {
        return employeeRepository
                .findAllByEmpPhoneAndEmpStatusOrderByEmpNoAsc(phone, ACTIVE);
    }

    private Employee findMatchingEmployee(
            PasswordRecoveryIdentityRequest request) {
        String empId = request.empId().trim().toLowerCase();
        String empName = request.empName().trim();
        String phone = PhoneNumberUtils.normalize(request.phone());

        return employeeRepository
                .findByEmpIdAndEmpNameAndEmpPhoneAndEmpStatus(
                        empId, empName, phone, ACTIVE)
                .orElseThrow(() -> new AccountRecoveryException(
                        "아이디, 이름, 휴대전화 번호와 일치하는 계정을 찾을 수 없습니다."));
    }
}
