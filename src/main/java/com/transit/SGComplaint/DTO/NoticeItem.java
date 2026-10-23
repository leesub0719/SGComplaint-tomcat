package com.transit.SGComplaint.DTO;

public record NoticeItem(
        Long noticeNo,
        String categoryLabel,
        String title,
        boolean pinned,
        boolean popup,
        String registeredDate,
        long viewCount) {
}
