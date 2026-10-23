package com.transit.SGComplaint.DTO;

public record AdminPartnerItem(
        Long partnerNo,
        String name,
        String phone,
        String site,
        String notes,
        String createdDate) {
}
