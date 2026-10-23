package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.MyPageApiResponse;
import com.transit.SGComplaint.service.PhoneVerificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 마이페이지 API 전용 예외 처리.
 *
 * <p>Thymeleaf 쪽의 {@code BindingResult} 처리와 같은 메시지를 JSON으로 변환한다.</p>
 */
@RestControllerAdvice(assignableTypes = MyPageApiController.class)
public class MyPageApiExceptionHandler {

    /** Bean Validation 실패 → 필드별 메시지 맵. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MyPageApiResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String first = fieldErrors.values().stream()
                .findFirst()
                .orElse("입력값을 확인해 주세요.");
        return ResponseEntity.badRequest()
                .body(MyPageApiResponse.fail(first, fieldErrors));
    }

    /** 휴대전화 인증 토큰이 없거나 만료된 경우. */
    @ExceptionHandler(PhoneVerificationException.class)
    public ResponseEntity<MyPageApiResponse> handlePhoneVerification(
            PhoneVerificationException exception) {
        return ResponseEntity.badRequest().body(
                MyPageApiResponse.fieldError(
                        "phoneVerificationToken", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MyPageApiResponse> handleIllegalArgument(
            IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(MyPageApiResponse.fail(exception.getMessage()));
    }

    /** 탈퇴 불가 상태 등. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<MyPageApiResponse> handleIllegalState(
            IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MyPageApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MyPageApiResponse> handleAccessDenied(
            AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MyPageApiResponse.fail(exception.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<MyPageApiResponse> handleNotFound(
            NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MyPageApiResponse.fail(exception.getMessage()));
    }
}
