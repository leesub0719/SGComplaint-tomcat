package com.transit.SGComplaint.service;

public class DuplicateEmployeeIdException extends RuntimeException {

    public DuplicateEmployeeIdException(String message) {
        super(message);
    }
}
