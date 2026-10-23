package com.transit.SGComplaint.DTO;

/**
 * 회원 관리 화면 상단 통계 + 검색 결과.
 */
public record AdminMemberSummary(
        long totalMemberCount,
        long activeMemberCount,
        long withdrawnMemberCount,
        long administratorCount,
        long masterCount,
        AdminPageResponse<AdminMemberItem> members
) {
}
