package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.PhoneVerificationResponse;
import com.transit.SGComplaint.service.PhoneVerificationException;
import com.transit.SGComplaint.sms.SmsSendException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PhoneVerificationController.class)
public class PhoneVerificationExceptionHandler {

    @ExceptionHandler(PhoneVerificationException.class)
    public ResponseEntity<PhoneVerificationResponse> handleVerification(
            PhoneVerificationException exception) {
        return ResponseEntity.badRequest().body(
                PhoneVerificationResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(SmsSendException.class)
    public ResponseEntity<PhoneVerificationResponse> handleSmsSend(
            SmsSendException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                PhoneVerificationResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PhoneVerificationResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값을 확인해 주세요.");
        return ResponseEntity.badRequest().body(
                PhoneVerificationResponse.failure(message));
    }
}
