package com.transit.SGComplaint.service;

import com.transit.SGComplaint.DTO.AdminMemberItem;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AdminMemberService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    private final EmployeeMapper employeeRepository;

    public AdminMemberService(EmployeeMapper employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public long countAllMembers() { return employeeRepository.count(); }
    public long countActiveMembers() { return employeeRepository.countByEmpStatus("Y"); }
    public long countWithdrawnMembers() { return employeeRepository.countByEmpStatus("N"); }
    public long countAdministrators() { return employeeRepository.countByEmpRole("A"); }
    public long countMasters() { return employeeRepository.countByEmpRole("M"); }

    public Page<AdminMemberItem> searchMembers(
            String status,
            String role,
            String keyword,
            String currentAdminId,
            int page) {
        String statusFilter = allowedValue(status, "Y", "N");
        String roleFilter = allowedRole(role);
        String keywordFilter = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;

        int requestedPage = Math.max(0, page);
        Page<Employee> result = employeeRepository.searchMembers(statusFilter, keywordFilter, roleFilter,
                PageRequest.of(requestedPage, 20));
        if (result.getTotalPages() > 0 && requestedPage >= result.getTotalPages()) {
            result = employeeRepository.searchMembers(statusFilter, keywordFilter, roleFilter,
                    PageRequest.of(result.getTotalPages() - 1, 20));
        }
        return result.map(employee -> toItem(employee, currentAdminId));
    }

    @Transactional
    public String changeMemberRole(
            String currentAdminId,
            Long targetEmpNo,
            String requestedRole) {
        Employee administrator = employeeRepository
                .findByEmpIdAndEmpStatus(currentAdminId, "Y")
                .filter(Employee::hasAdminRole)
                .orElseThrow(() -> new IllegalStateException(
                        "관리자 권한을 확인할 수 없습니다. 다시 로그인해 주세요."
                ));
        String role = allowedRole(requestedRole);
        if (role == null) {
            throw new IllegalArgumentException("변경할 권한을 선택해 주세요.");
        }

        Employee target = employeeRepository.findById(targetEmpNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "변경할 회원을 찾을 수 없습니다."
                ));
        if (administrator.getEmpNo().equals(target.getEmpNo())
                && !role.equals(target.getEmpRole())) {
            throw new IllegalArgumentException(
                    "현재 로그인한 계정 자신의 권한은 변경할 수 없습니다."
            );
        }
        if (("M".equals(role) || target.isMaster()) && !administrator.isMaster()) {
            throw new IllegalArgumentException(
                    "마스터 권한의 부여 및 변경은 마스터 계정만 할 수 있습니다."
            );
        }
        if (role.equals(target.getEmpRole())) {
            return target.getEmpName() + " 회원은 이미 " + roleLabel(role) + " 권한입니다.";
        }

        target.changeRole(role);
        employeeRepository.save(target);
        return target.getEmpName() + " 회원의 권한을 " + roleLabel(role)
                + "(으)로 변경했습니다. 해당 회원은 다시 로그인해야 새 권한이 적용됩니다.";
    }

    private AdminMemberItem toItem(Employee employee, String currentAdminId) {
        return new AdminMemberItem(
                employee.getEmpNo(),
                employee.getEmpId(),
                employee.getEmpName(),
                employee.getEmpEmail(),
                employee.getEmpPhone(),
                employee.getEmpAddress(),
                employee.getEmpRole(),
                roleLabel(employee.getEmpRole()),
                employee.getEmpStatus(),
                "Y".equals(employee.getEmpStatus()) ? "이용중" : "탈퇴",
                employee.getCreatedAt().format(DATE_FORMATTER),
                employee.getCreatedAt().format(DATE_TIME_FORMATTER),
                employee.getUpdatedAt().format(DATE_TIME_FORMATTER),
                employee.getEmpId().equals(currentAdminId),
                employee.getWithdrawalOriginalId()
        );
    }

    private String allowedValue(String value, String first, String second) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return first.equals(normalized) || second.equals(normalized)
                ? normalized
                : null;
    }

    private String allowedRole(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "U", "A", "M" -> normalized;
            default -> null;
        };
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "M" -> "마스터";
            case "A" -> "관리자";
            default -> "사용자";
        };
    }
}
