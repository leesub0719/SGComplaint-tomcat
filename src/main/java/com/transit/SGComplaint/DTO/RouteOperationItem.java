package com.transit.SGComplaint.DTO;

public record RouteOperationItem(
        Long routeNo,
        String routeType,
        String routeTypeLabel,
        String busName,
        String terminalInfo,
        String dispatchInterval,
        String inquiryPhone,
        String routeUrl) {
}
