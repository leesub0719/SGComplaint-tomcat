package com.transit.SGComplaint.DTO;

import com.transit.SGComplaint.domain.Employee;

/**
 * 마이페이지 초기 로딩용 REST 응답.
 *
 * <p>{@code verified}가 false면 비밀번호 재확인 화면을 먼저 보여줘야 하며,
 * 이때 개인정보(이메일·연락처·주소)는 내려보내지 않는다.</p>
 */
public record MyPageProfileResponse(
        boolean verified,
        long verificationRemainingMillis,
        String empId,
        String empName,
        String empEmail,
        String empPhone,
        String postcode,
        String address,
        String addressDetail
) {

    /** 재확인 전 상태: 아이디/이름만 노출한다. */
    public static MyPageProfileResponse locked(Employee employee) {
        return new MyPageProfileResponse(
                false, 0L,
                employee.getEmpId(),
                employee.getEmpName(),
                null, null, null, null, null);
    }

    /** 재확인 완료 상태: 수정 폼에 채울 값 전체를 내려보낸다. */
    public static MyPageProfileResponse unlocked(
            Employee employee,
            MemberProfileUpdateRequest form,
            long remainingMillis) {
        return new MyPageProfileResponse(
                true,
                remainingMillis,
                employee.getEmpId(),
                employee.getEmpName(),
                form.getEmpEmail(),
                form.getEmpPhone(),
                form.getPostcode(),
                form.getAddress(),
                form.getAddressDetail());
    }
}
