package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.AccountRecoveryResponse;
import com.transit.SGComplaint.service.AccountRecoveryException;
import com.transit.SGComplaint.service.PhoneVerificationException;
import com.transit.SGComplaint.sms.SmsSendException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AccountRecoveryController.class)
public class AccountRecoveryExceptionHandler {

    @ExceptionHandler({AccountRecoveryException.class, PhoneVerificationException.class})
    public ResponseEntity<AccountRecoveryResponse> handleBadRequest(
            RuntimeException exception) {
        return ResponseEntity.badRequest().body(
                AccountRecoveryResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(SmsSendException.class)
    public ResponseEntity<AccountRecoveryResponse> handleSmsSend(
            SmsSendException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                AccountRecoveryResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AccountRecoveryResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값을 확인해 주세요.");
        return ResponseEntity.badRequest().body(
                AccountRecoveryResponse.failure(message));
    }
}
