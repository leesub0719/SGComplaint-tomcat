package com.transit.SGComplaint.service;

/**
 * 지원서 첨부파일 검증 실패 시 어느 파일 입력 항목을 다시 선택해야 하는지 전달한다.
 */
public class ApplicationFileValidationException extends IllegalArgumentException {
    private final String fieldName;

    public ApplicationFileValidationException(String fieldName, String message, Throwable cause) {
        super(message, cause);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
