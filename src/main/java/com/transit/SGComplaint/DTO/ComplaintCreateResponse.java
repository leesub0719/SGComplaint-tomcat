package com.transit.SGComplaint.DTO;

public record ComplaintCreateResponse(
        boolean success,
        String message,
        Long complaintNo) {

    public static ComplaintCreateResponse success(Long complaintNo) {
        return new ComplaintCreateResponse(
                true,
                "민원이 정상적으로 접수되었습니다.",
                complaintNo
        );
    }

    public static ComplaintCreateResponse failure(String message) {
        return new ComplaintCreateResponse(false, message, null);
    }
}
