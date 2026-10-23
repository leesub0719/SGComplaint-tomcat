package com.transit.SGComplaint.DTO;

public record ComplaintPasswordResponse(boolean success, String message, String redirectUrl) {

    public static ComplaintPasswordResponse success(Long complaintNo) {
        return new ComplaintPasswordResponse(
                true,
                "비밀번호가 확인되었습니다.",
                "/complaints/view/" + complaintNo
        );
    }

    public static ComplaintPasswordResponse failure(String message) {
        return new ComplaintPasswordResponse(false, message, null);
    }
}
