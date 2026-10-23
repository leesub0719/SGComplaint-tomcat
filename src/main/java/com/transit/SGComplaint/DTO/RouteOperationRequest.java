package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RouteOperationRequest {

    @NotBlank(message = "운행 유형을 선택해 주세요.")
    @Pattern(regexp = "VILLAGE|DDOK", message = "올바른 운행 유형을 선택해 주세요.")
    private String routeType;

    @NotBlank(message = "버스 이름을 입력해 주세요.")
    @Size(max = 100, message = "버스 이름은 100자 이내로 입력해 주세요.")
    private String busName;

    @NotBlank(message = "기점-종점을 입력해 주세요.")
    @Size(max = 200, message = "기점-종점은 200자 이내로 입력해 주세요.")
    private String terminalInfo;

    @NotBlank(message = "배차간격을 입력해 주세요.")
    @Size(max = 200, message = "배차간격은 200자 이내로 입력해 주세요.")
    private String dispatchInterval;

    @NotBlank(message = "문의전화를 입력해 주세요.")
    @Size(max = 100, message = "문의전화는 100자 이내로 입력해 주세요.")
    private String inquiryPhone;

    @NotBlank(message = "노선안내 링크를 입력해 주세요.")
    @Size(max = 500, message = "노선안내 링크는 500자 이내로 입력해 주세요.")
    private String routeUrl;

    public String getRouteType() { return routeType; }
    public void setRouteType(String routeType) { this.routeType = routeType; }
    public String getBusName() { return busName; }
    public void setBusName(String busName) { this.busName = busName; }
    public String getTerminalInfo() { return terminalInfo; }
    public void setTerminalInfo(String terminalInfo) { this.terminalInfo = terminalInfo; }
    public String getDispatchInterval() { return dispatchInterval; }
    public void setDispatchInterval(String dispatchInterval) { this.dispatchInterval = dispatchInterval; }
    public String getInquiryPhone() { return inquiryPhone; }
    public void setInquiryPhone(String inquiryPhone) { this.inquiryPhone = inquiryPhone; }
    public String getRouteUrl() { return routeUrl; }
    public void setRouteUrl(String routeUrl) { this.routeUrl = routeUrl; }
}
