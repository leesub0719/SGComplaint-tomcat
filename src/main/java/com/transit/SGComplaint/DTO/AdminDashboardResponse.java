package com.transit.SGComplaint.DTO;

/**
 * 관리자 대시보드 상단 통계 + 최근 민원 목록.
 *
 * <p>기존 {@code AdminController.dashboard()}가 Model에 6개 이상 나눠 담던 값을
 * 한 번의 요청으로 묶어 내려준다.</p>
 */
public record AdminDashboardResponse(
        long totalComplaints,
        long checkingCount,
        long processingCount,
        long completedCount,
        long activeMemberCount,
        String selectedStatus,
        AdminPageResponse<AdminComplaintItem> complaints
) {
}
