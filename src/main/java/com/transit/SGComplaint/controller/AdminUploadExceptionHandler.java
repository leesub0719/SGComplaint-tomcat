package com.transit.SGComplaint.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = {
        AdminController.class, AdminMainPageController.class, AdminRouteOperationController.class
})
public class AdminUploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> tooLarge() {
        return ResponseEntity.status(413).contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body("파일 크기가 허용 범위를 초과했습니다. 파일 한 개는 최대 10MB(공지 이미지 5MB), "
                        + "요청 전체는 최대 52MB입니다. 이전 화면으로 돌아가 파일 크기를 줄여 주세요.");
    }
}
