package com.transit.SGComplaint.service;

import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.mapper.EmployeeMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeMapper employeeRepository;

    public EmployeeUserDetailsService(EmployeeMapper employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String empId) {
        String normalizedEmpId = empId == null
                ? ""
                : empId.trim().toLowerCase(Locale.ROOT);

        Employee employee = employeeRepository.findByEmpId(normalizedEmpId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "등록되지 않은 아이디입니다."
                ));

        String role = switch (employee.getEmpRole()) {
            case "M" -> "MASTER";
            case "A" -> "ADMIN";
            default -> "USER";
        };

        return User.withUsername(employee.getEmpId())
                .password(employee.getEmpPassword())
                .roles(role)
                .disabled(!"Y".equals(employee.getEmpStatus()))
                .build();
    }
}
