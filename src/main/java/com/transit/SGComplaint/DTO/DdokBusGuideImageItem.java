package com.transit.SGComplaint.DTO;

public record DdokBusGuideImageItem(
        Long imageNo,
        String originalName,
        String formattedSize,
        String createdDateTime,
        String imageUrl) {
}
