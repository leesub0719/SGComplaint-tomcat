package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class RouteOperation {
    private Long routeNo;
    private String routeType;
    private String busName;
    private String terminalInfo;
    private String dispatchInterval;
    private String inquiryPhone;
    private String routeUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected RouteOperation() {
    }

    public static RouteOperation create(
            String routeType,
            String busName,
            String terminalInfo,
            String dispatchInterval,
            String inquiryPhone,
            String routeUrl) {
        RouteOperation operation = new RouteOperation();
        operation.changeInformation(
                routeType, busName, terminalInfo,
                dispatchInterval, inquiryPhone, routeUrl);
        operation.status = "Y";
        return operation;
    }

    public void changeInformation(
            String routeType,
            String busName,
            String terminalInfo,
            String dispatchInterval,
            String inquiryPhone,
            String routeUrl) {
        this.routeType = routeType;
        this.busName = busName.trim();
        this.terminalInfo = terminalInfo.trim();
        this.dispatchInterval = dispatchInterval.trim();
        this.inquiryPhone = inquiryPhone.trim();
        this.routeUrl = routeUrl;
    }

    public void deactivate() {
        status = "N";
    }
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        createdAt = now;
        updatedAt = now;
    }
    private void onUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getRouteNo() { return routeNo; }
    public String getRouteType() { return routeType; }
    public String getBusName() { return busName; }
    public String getTerminalInfo() { return terminalInfo; }
    public String getDispatchInterval() { return dispatchInterval; }
    public String getInquiryPhone() { return inquiryPhone; }
    public String getRouteUrl() { return routeUrl; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
