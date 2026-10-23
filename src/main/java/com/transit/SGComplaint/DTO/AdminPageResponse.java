package com.transit.SGComplaint.DTO;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 목록 공통 응답.
 *
 * <p>Spring Data의 {@code Page}를 그대로 직렬화하면 내부 구조(pageable, sort 등)까지
 * 노출되고 버전에 따라 형태가 달라진다. 화면 API가 사용하는 필드만 골라 고정한다.</p>
 *
 * <p>{@code PublicComplaintPageResponse}와 같은 모양이며, 그쪽도 나중에
 * 이 타입으로 통일할 수 있다.</p>
 */
public record AdminPageResponse<T>(
        List<T> items,
        int page,
        int size,
        int totalPages,
        long totalElements,
        boolean first,
        boolean last
) {

    public static <T> AdminPageResponse<T> from(Page<T> page) {
        return new AdminPageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.isLast());
    }
}
