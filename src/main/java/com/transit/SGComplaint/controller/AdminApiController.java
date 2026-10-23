package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.AdminApiResponse;
import com.transit.SGComplaint.DTO.AdminComplaintItem;
import com.transit.SGComplaint.DTO.AdminDashboardResponse;
import com.transit.SGComplaint.DTO.AdminMemberItem;
import com.transit.SGComplaint.DTO.AdminMemberSummary;
import com.transit.SGComplaint.DTO.AdminPageResponse;
import com.transit.SGComplaint.domain.ComplaintStatus;
import com.transit.SGComplaint.service.AdminComplaintService;
import com.transit.SGComplaint.service.AdminMemberService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 관리자 화면의 JSON API.
 *
 * <p>기존 {@link AdminController}는 Thymeleaf 폼 제출 후 리다이렉트하는 화면
 * 컨트롤러다. 이 컨트롤러는 같은 서비스({@link AdminComplaintService},
 * {@link AdminMemberService})를 호출하되 JavaScript와 외부 시스템이 사용할 수
 * 있도록 결과를 JSON으로 반환한다.</p>
 *
 * <p>대상 범위는 대시보드 / 민원 처리 / 회원 관리 세 화면이다. 공지·파트너·노선
 * 관리는 같은 패턴(목록 조회 + 저장/삭제)이라 동일한 방식으로 확장할 수 있다.</p>
 *
 * <p>접근 권한은 {@code SecurityConfig}에서 ADMIN 또는 MASTER로 제한한다.</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final AdminComplaintService adminComplaintService;
    private final AdminMemberService adminMemberService;

    public AdminApiController(
            AdminComplaintService adminComplaintService,
            AdminMemberService adminMemberService) {
        this.adminComplaintService = adminComplaintService;
        this.adminMemberService = adminMemberService;
    }

    /** 대시보드: 상태별 건수와 최근 민원 페이지를 한 번에 돌려준다. */
    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard(
            @RequestParam(name = "status", required = false) String statusValue,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        ComplaintStatus selectedStatus = parseStatus(statusValue);
        Page<AdminComplaintItem> complaints =
                adminComplaintService.getDashboardComplaints(selectedStatus, page);

        return new AdminDashboardResponse(
                adminComplaintService.countAllComplaints(),
                adminComplaintService.countComplaints(ComplaintStatus.CHECKING),
                adminComplaintService.countComplaints(ComplaintStatus.PROCESSING),
                adminComplaintService.countComplaints(ComplaintStatus.COMPLETED),
                adminComplaintService.countActiveMembers(),
                selectedStatus == null ? "ALL" : selectedStatus.name(),
                AdminPageResponse.from(complaints));
    }

    /** 민원 처리 목록. 상태 필터만 있고 페이징은 없다(기존 화면과 동일). */
    @GetMapping("/complaints")
    public List<AdminComplaintItem> complaints(
            @RequestParam(name = "status", required = false) String statusValue) {
        return adminComplaintService.getComplaints(parseStatus(statusValue));
    }

    /**
     * 답변 등록 및 처리상태 변경.
     *
     * <p>답변 첨부파일이 있으므로 JSON이 아니라 multipart/form-data로 받는다.</p>
     */
    @PostMapping("/complaints/{complaintNo}")
    public AdminApiResponse answerComplaint(
            Authentication authentication,
            @PathVariable(name = "complaintNo") Long complaintNo,
            @RequestParam(name = "status") ComplaintStatus status,
            @RequestParam(name = "version") Long version,
            @RequestParam(name = "answerContent") String answerContent,
            @RequestParam(name = "answerAttachments", required = false)
            List<MultipartFile> answerAttachments) {
        adminComplaintService.answerComplaint(
                authentication.getName(),
                complaintNo,
                status,
                version,
                answerContent,
                answerAttachments);
        return AdminApiResponse.ok("민원 답변과 처리상태가 저장되었습니다.");
    }

    /** 클라이언트가 상태 이름을 하드코딩하지 않도록 코드와 라벨을 함께 반환한다. */
    @GetMapping("/complaint-statuses")
    public List<Map<String, String>> complaintStatuses() {
        return java.util.Arrays.stream(ComplaintStatus.values())
                .map(status -> Map.of("code", status.name(), "label", status.getLabel()))
                .toList();
    }

    /** 회원 검색 + 상단 통계. */
    @GetMapping("/members")
    public AdminMemberSummary members(
            Authentication authentication,
            @RequestParam(name = "memberStatus", defaultValue = "ALL") String memberStatus,
            @RequestParam(name = "memberRole", defaultValue = "ALL") String memberRole,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Page<AdminMemberItem> members = adminMemberService.searchMembers(
                memberStatus, memberRole, keyword, authentication.getName(), page);

        return new AdminMemberSummary(
                adminMemberService.countAllMembers(),
                adminMemberService.countActiveMembers(),
                adminMemberService.countWithdrawnMembers(),
                adminMemberService.countAdministrators(),
                adminMemberService.countMasters(),
                AdminPageResponse.from(members));
    }

    /** 회원 권한 변경. 서비스가 결과 메시지를 만들어 돌려준다. */
    @PostMapping("/members/{empNo}/role")
    public AdminApiResponse changeMemberRole(
            Authentication authentication,
            @PathVariable(name = "empNo") Long empNo,
            @RequestBody RoleChangeRequest request) {
        String message = adminMemberService.changeMemberRole(
                authentication.getName(), empNo, request.role());
        return AdminApiResponse.ok(message);
    }

    public record RoleChangeRequest(String role) {
    }

    /** "ALL"이나 알 수 없는 값은 null(전체)로 처리한다. 기존 컨트롤러와 같은 규칙. */
    private ComplaintStatus parseStatus(String statusValue) {
        if (statusValue == null || statusValue.isBlank()
                || "ALL".equalsIgnoreCase(statusValue)) {
            return null;
        }
        try {
            return ComplaintStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
