package com.transit.SGComplaint.service;

/** 다른 관리자가 먼저 같은 민원을 저장했을 때 발생하는 충돌 예외. */
public class ComplaintConflictException extends ComplaintException {

    public ComplaintConflictException(String message) {
        super(message);
    }
}
