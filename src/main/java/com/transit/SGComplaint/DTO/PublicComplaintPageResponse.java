package com.transit.SGComplaint.DTO;

import java.util.List;

public record PublicComplaintPageResponse(
        List<PublicComplaintItem> items,
        int page,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean first,
        boolean last) {
}
