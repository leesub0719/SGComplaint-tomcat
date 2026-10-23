package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.MemberPasswordConfirmRequest;
import com.transit.SGComplaint.DTO.MemberProfileUpdateRequest;
import com.transit.SGComplaint.DTO.MyPageApiResponse;
import com.transit.SGComplaint.DTO.MyPageProfileResponse;
import com.transit.SGComplaint.DTO.ComplaintListItem;
import com.transit.SGComplaint.DTO.ComplaintUpdateRequest;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.service.ComplaintService;
import com.transit.SGComplaint.service.EmployeeService;
import com.transit.SGComplaint.service.ProfileVerificationSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 마이페이지 정보수정 화면의 JSON API.
 *
 * <p>기존 {@link MyPageController}(폼 submit + redirect)와 같은 서비스·세션 게이트를
 * 사용하므로 Thymeleaf 화면과 JavaScript 요청의 인증 상태가 어긋나지 않는다.</p>
 */
@RestController
@RequestMapping("/api/mypage")
public class MyPageApiController {

    private final EmployeeService employeeService;
    private final ComplaintService complaintService;
    private final ProfileVerificationSession verificationSession;

    public MyPageApiController(
            EmployeeService employeeService,
            ComplaintService complaintService,
            ProfileVerificationSession verificationSession) {
        this.employeeService = employeeService;
        this.complaintService = complaintService;
        this.verificationSession = verificationSession;
    }

    @GetMapping("/inquiries")
    public Page<ComplaintListItem> inquiries(
            Authentication authentication,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Employee employee = employeeService.getRequiredActiveEmployee(authentication.getName());
        DateRange range = normalizeRange(startDate, endDate);
        return complaintService.getMemberComplaints(
                employee.getEmpNo(), range.start(), range.end(), page);
    }

    @PutMapping("/inquiries/{complaintNo}")
    public MyPageApiResponse updateInquiry(
            Authentication authentication,
            @PathVariable("complaintNo") Long complaintNo,
            @Valid @RequestBody ComplaintUpdateRequest request) {
        Employee employee = employeeService.getRequiredActiveEmployee(authentication.getName());
        complaintService.updateMemberComplaint(employee.getEmpNo(), complaintNo, request);
        return MyPageApiResponse.ok("문의가 수정되었습니다.");
    }

    @DeleteMapping("/inquiries/{complaintNo}")
    public MyPageApiResponse deleteInquiry(
            Authentication authentication,
            @PathVariable("complaintNo") Long complaintNo) {
        Employee employee = employeeService.getRequiredActiveEmployee(authentication.getName());
        complaintService.deleteMemberComplaint(employee.getEmpNo(), complaintNo);
        return MyPageApiResponse.ok("문의가 삭제되었습니다.");
    }

    private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate normalizedEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate normalizedStart = startDate == null
                ? normalizedEnd.minusYears(1)
                : startDate;
        if (normalizedStart.isAfter(normalizedEnd)) {
            return new DateRange(normalizedEnd, normalizedStart);
        }
        return new DateRange(normalizedStart, normalizedEnd);
    }

    private record DateRange(LocalDate start, LocalDate end) { }

    /** 초기 로딩: 재확인 통과 여부 + (통과했다면) 수정 폼 초기값. */
    @GetMapping("/profile")
    public MyPageProfileResponse profile(
            Authentication authentication,
            HttpSession session) {
        Employee employee = employeeService.getRequiredActiveEmployee(authentication.getName());
        if (!verificationSession.isVerified(session)) {
            return MyPageProfileResponse.locked(employee);
        }
        return MyPageProfileResponse.unlocked(
                employee,
                employeeService.getProfileUpdateForm(authentication.getName()),
                verificationSession.remainingMillis(session));
    }

    /** 비밀번호 재확인. 성공하면 세션 게이트를 10분간 열어준다. */
    @PostMapping("/profile/confirm-password")
    public ResponseEntity<MyPageApiResponse> confirmPassword(
            Authentication authentication,
            @Valid @RequestBody MemberPasswordConfirmRequest request,
            HttpSession session) {
        if (!employeeService.matchesCurrentPassword(
                authentication.getName(), request.getCurrentPassword())) {
            return ResponseEntity.badRequest().body(
                    MyPageApiResponse.fieldError(
                            "currentPassword", "현재 비밀번호가 일치하지 않습니다."));
        }
        verificationSession.markVerified(session);
        return ResponseEntity.ok(MyPageApiResponse.ok("비밀번호가 확인되었습니다."));
    }

    /** 회원정보 저장. */
    @PostMapping("/profile")
    public ResponseEntity<MyPageApiResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody MemberProfileUpdateRequest request,
            HttpSession session) {
        if (!verificationSession.isVerified(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    MyPageApiResponse.fail("비밀번호 재확인이 만료되었습니다. 다시 확인해 주세요."));
        }
        if (!request.passwordMatches()) {
            return ResponseEntity.badRequest().body(
                    MyPageApiResponse.fieldError(
                            "newPasswordConfirm",
                            "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."));
        }

        employeeService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(MyPageApiResponse.ok("회원정보가 저장되었습니다."));
    }

    /** 회원탈퇴. 성공하면 인증 정보와 세션을 정리한다. */
    @PostMapping("/withdraw")
    public ResponseEntity<MyPageApiResponse> withdraw(
            Authentication authentication,
            HttpServletRequest servletRequest,
            HttpSession session) {
        if (!verificationSession.isVerified(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    MyPageApiResponse.fail("비밀번호 재확인이 만료되었습니다. 다시 확인해 주세요."));
        }

        employeeService.withdraw(authentication.getName());

        SecurityContextHolder.clearContext();
        HttpSession current = servletRequest.getSession(false);
        if (current != null) {
            current.invalidate();
        }
        return ResponseEntity.ok(MyPageApiResponse.ok("회원탈퇴가 완료되었습니다."));
    }
}
