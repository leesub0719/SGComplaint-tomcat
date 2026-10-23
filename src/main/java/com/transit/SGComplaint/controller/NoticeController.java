package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.NoticeItem;
import com.transit.SGComplaint.DTO.StoredAttachment;
import com.transit.SGComplaint.service.UploadValidation;
import com.transit.SGComplaint.service.NoticeException;
import com.transit.SGComplaint.service.NoticeService;
import org.springframework.data.domain.Page;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public String notices(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        Page<NoticeItem> noticePage = noticeService.getNoticePage(keyword, page);
        model.addAttribute("noticePage", noticePage);
        model.addAttribute("notices", noticePage.getContent());
        model.addAttribute("keyword", keyword);
        return "notice/list";
    }

    @GetMapping("/{noticeNo}")
    public String noticeDetail(
            @PathVariable(name = "noticeNo") Long noticeNo,
            Model model) {
        try {
            model.addAttribute("notice", noticeService.getNoticeDetail(noticeNo));
            return "notice/detail";
        } catch (NoticeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @GetMapping("/images/{noticeImageNo}")
    public ResponseEntity<Resource> noticeImage(
            @PathVariable(name = "noticeImageNo") Long noticeImageNo) {
        try {
            StoredAttachment image = noticeService.getNoticeImage(noticeImageNo);
            FileSystemResource resource = new FileSystemResource(image.path());
            MediaType mediaType = MediaType.parseMediaType(UploadValidation.imageContentType(image.path().getFileName().toString()));
            String disposition = ContentDisposition.inline()
                    .filename(image.originalName(), StandardCharsets.UTF_8)
                    .build().toString();
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (NoticeException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공지사항 이미지를 찾을 수 없습니다.", exception);
        }
    }
}
