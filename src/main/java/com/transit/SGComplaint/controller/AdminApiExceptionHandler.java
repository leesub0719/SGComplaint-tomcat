package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.AdminApiResponse;
import com.transit.SGComplaint.service.ComplaintException;
import com.transit.SGComplaint.service.ComplaintConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 관리자 API 전용 예외 처리.
 *
 * <p>기존 컨트롤러가 try/catch로 잡아 errorMessage 플래시 속성에 담던 예외들을
 * 같은 메시지의 JSON 응답으로 변환한다.</p>
 */
@RestControllerAdvice(assignableTypes = AdminApiController.class)
public class AdminApiExceptionHandler {

    /** 같은 민원을 다른 관리자가 먼저 저장한 경우. */
    @ExceptionHandler(ComplaintConflictException.class)
    public ResponseEntity<AdminApiResponse> handleConflict(
            ComplaintConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(AdminApiResponse.fail(exception.getMessage()));
    }

    /** 답변 등록 실패(첨부 검증, 상태 전이 등). */
    @ExceptionHandler(ComplaintException.class)
    public ResponseEntity<AdminApiResponse> handleComplaint(ComplaintException exception) {
        return ResponseEntity.badRequest()
                .body(AdminApiResponse.fail(exception.getMessage()));
    }

    /** 권한 변경 시 잘못된 값. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AdminApiResponse> handleIllegalArgument(
            IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(AdminApiResponse.fail(exception.getMessage()));
    }

    /** 마지막 MASTER의 권한을 내리는 등 허용되지 않는 상태 변경. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AdminApiResponse> handleIllegalState(
            IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(AdminApiResponse.fail(exception.getMessage()));
    }
}
