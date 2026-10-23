package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.SignupApiResponse;
import com.transit.SGComplaint.service.DuplicateEmployeeIdException;
import com.transit.SGComplaint.service.PhoneVerificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 회원가입 API 전용 예외 처리.
 *
 * <p>Thymeleaf 컨트롤러가 {@code bindingResult.rejectValue(...)}로 붙이던 필드
 * 오류를 그대로 JSON {@code fieldErrors}로 옮긴다.</p>
 */
@RestControllerAdvice(assignableTypes = SignupApiController.class)
public class SignupApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SignupApiResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String first = fieldErrors.values().stream()
                .findFirst()
                .orElse("입력값을 확인해 주세요.");
        return ResponseEntity.badRequest()
                .body(SignupApiResponse.fail(first, fieldErrors));
    }

    /** 아이디 중복 → empId 필드 오류. */
    @ExceptionHandler(DuplicateEmployeeIdException.class)
    public ResponseEntity<SignupApiResponse> handleDuplicateId(
            DuplicateEmployeeIdException exception) {
        return ResponseEntity.badRequest()
                .body(SignupApiResponse.fieldError("empId", exception.getMessage()));
    }

    @ExceptionHandler(PhoneVerificationException.class)
    public ResponseEntity<SignupApiResponse> handlePhoneVerification(
            PhoneVerificationException exception) {
        return ResponseEntity.badRequest()
                .body(SignupApiResponse.fieldError("empPhone", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SignupApiResponse> handleIllegalArgument(
            IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(SignupApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<SignupApiResponse> handleIllegalState(
            IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(SignupApiResponse.fail(exception.getMessage()));
    }
}
