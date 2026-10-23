package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.ComplaintCreateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = ComplaintController.class)
public class ComplaintExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ComplaintCreateResponse> handleMaxUploadSize() {
        return ResponseEntity.badRequest().body(
                ComplaintCreateResponse.failure(
                        "첨부파일 전체 크기가 허용 범위를 초과했습니다."
                )
        );
    }
}
