package com.transit.SGComplaint.domain;

public enum ComplaintStatus {

    CHECKING("확인중", "민원을 확인중입니다."),
    PROCESSING("처리중", "민원을 처리중입니다."),
    COMPLETED("답변완료", "등록해주신 민원에 대해 처리가 완료 되었습니다.");

    private final String label;
    private final String defaultAnswer;

    ComplaintStatus(String label, String defaultAnswer) {
        this.label = label;
        this.defaultAnswer = defaultAnswer;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultAnswer() {
        return defaultAnswer;
    }
}
