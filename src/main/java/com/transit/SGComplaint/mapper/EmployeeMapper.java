package com.transit.SGComplaint.mapper;

import com.transit.SGComplaint.domain.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Mapper
public interface EmployeeMapper {
    boolean existsByEmpId(@Param("empId") String empId);
    boolean existsByEmpIdAndEmpStatus(@Param("empId") String empId, @Param("empStatus") String empStatus);
    Optional<Employee> findByEmpId(@Param("empId") String empId);
    Optional<Employee> findByEmpIdAndEmpStatus(@Param("empId") String empId, @Param("empStatus") String empStatus);
    List<Employee> findAllByEmpPhoneAndEmpStatusOrderByEmpNoAsc(@Param("empPhone") String empPhone, @Param("empStatus") String empStatus);
    Optional<Employee> findByEmpIdAndEmpNameAndEmpPhoneAndEmpStatus(@Param("empId") String empId, @Param("empName") String empName, @Param("empPhone") String empPhone, @Param("empStatus") String empStatus);
    Optional<Employee> findById(@Param("empNo") Long empNo);
    long count();
    long countByEmpStatus(@Param("empStatus") String empStatus);
    long countByEmpRole(@Param("empRole") String empRole);
    List<Employee> selectMemberPage(@Param("status") String status,
                                    @Param("keyword") String keyword,
                                    @Param("role") String role,
                                    @Param("limit") int limit,
                                    @Param("offset") long offset);
    long countMemberPage(@Param("status") String status,
                         @Param("keyword") String keyword,
                         @Param("role") String role);
    int insertEmployee(Employee employee);
    int updateEmployee(Employee employee);
    int releaseWithdrawnEmployeeId(@Param("empId") String empId, @Param("withdrawnId") String withdrawnId);
    int anonymizeExpiredWithdrawn(@Param("cutoff") java.time.LocalDateTime cutoff,
                                  @Param("limit") int limit);
    default Page<Employee> searchMembers(String status, String keyword, String role, Pageable pageable) {
        return new PageImpl<>(selectMemberPage(status, keyword, role, pageable.getPageSize(), pageable.getOffset()),
                pageable, countMemberPage(status, keyword, role));
    }
    default Employee saveAndFlush(Employee employee) { insertEmployee(employee); return employee; }
    default Employee save(Employee employee) { updateEmployee(employee); return employee; }
}
