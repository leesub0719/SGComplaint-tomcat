package com.transit.SGComplaint.DTO;

public record NoticeDetail(
        Long noticeNo,
        String categoryLabel,
        String title,
        String content,
        String administratorName,
        boolean pinned,
        String registeredDateTime,
        long viewCount) {
}
