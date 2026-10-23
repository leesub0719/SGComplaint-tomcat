package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.ComplaintCreateRequest;
import com.transit.SGComplaint.DTO.ComplaintCreateResponse;
import com.transit.SGComplaint.DTO.ComplaintPasswordResponse;
import com.transit.SGComplaint.DTO.PublicComplaintItem;
import com.transit.SGComplaint.DTO.StoredAttachment;
import com.transit.SGComplaint.service.ComplaintException;
import com.transit.SGComplaint.service.ComplaintService;
import com.transit.SGComplaint.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/complaints")
public class ComplaintController {

    private final EmployeeService employeeService;
    private final ComplaintService complaintService;

    public ComplaintController(
            EmployeeService employeeService,
            ComplaintService complaintService) {
        this.employeeService = employeeService;
        this.complaintService = complaintService;
    }

    @GetMapping
    public String complaintList(
            @RequestParam(name = "category", defaultValue = "ALL") String category,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            CsrfToken csrfToken,
            Model model) {
        // The password form is rendered near the end of the streamed template.
        // Resolve the deferred token before the response can be committed.
        csrfToken.getToken();
        String normalizedCategory = switch (category.toUpperCase()) {
            case "PRAISE", "COMPLAINT", "LOST" -> category.toUpperCase();
            default -> "ALL";
        };
        Page<PublicComplaintItem> complaintPage =
                complaintService.getPublicComplaints(normalizedCategory, keyword, page);
        model.addAttribute("complaintPage", complaintPage);
        model.addAttribute("complaints", complaintPage.getContent());
        model.addAttribute("selectedCategory", normalizedCategory);
        model.addAttribute("boardTitle", switch (normalizedCategory) {
            case "PRAISE" -> "칭찬합니다";
            case "COMPLAINT" -> "불편합니다";
            case "LOST" -> "분실물 문의";
            default -> "전체 민원";
        });
        model.addAttribute("keyword", keyword);
        return "complaint/list";
    }

    @PostMapping("/{complaintNo}/verify")
    @ResponseBody
    public ResponseEntity<ComplaintPasswordResponse> verifyComplaintPassword(
            @PathVariable(name = "complaintNo") Long complaintNo,
            @RequestParam(name = "password") String password,
            HttpSession session) {
        try {
            Long lockedUntil = (Long) session.getAttribute(lockSessionKey(complaintNo));
            if (lockedUntil != null && lockedUntil > System.currentTimeMillis()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ComplaintPasswordResponse.failure("비밀번호 입력을 여러 번 실패했습니다. 1분 후 다시 시도해 주세요."));
            }
            if (!complaintService.verifyPublicPassword(complaintNo, password)) {
                int failures = session.getAttribute(failureSessionKey(complaintNo)) instanceof Integer count
                        ? count + 1 : 1;
                if (failures >= 5) {
                    session.removeAttribute(failureSessionKey(complaintNo));
                    session.setAttribute(lockSessionKey(complaintNo), System.currentTimeMillis() + 60_000L);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(ComplaintPasswordResponse.failure("비밀번호 입력을 5회 실패했습니다. 1분 후 다시 시도해 주세요."));
                }
                session.setAttribute(failureSessionKey(complaintNo), failures);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ComplaintPasswordResponse.failure("게시글 비밀번호가 일치하지 않습니다."));
            }
            session.removeAttribute(failureSessionKey(complaintNo));
            session.removeAttribute(lockSessionKey(complaintNo));
            session.setAttribute(verifiedSessionKey(complaintNo), Boolean.TRUE);
            return ResponseEntity.ok(ComplaintPasswordResponse.success(complaintNo));
        } catch (ComplaintException exception) {
            return ResponseEntity.badRequest()
                    .body(ComplaintPasswordResponse.failure(exception.getMessage()));
        }
    }

    @GetMapping("/view/{complaintNo}")
    public String complaintDetail(
            @PathVariable(name = "complaintNo") Long complaintNo,
            Authentication authentication,
            HttpSession session,
            Model model) {
        requireVerified(session, complaintNo);
        model.addAttribute("complaint", complaintService.getPublicComplaint(complaintNo));
        model.addAttribute("canEdit", authentication != null
                && complaintService.canEditPublicComplaint(authentication.getName(), complaintNo));
        return "complaint/detail";
    }

    @GetMapping("/view/{complaintNo}/attachments/{attachmentNo}")
    public ResponseEntity<Resource> downloadComplaintAttachment(
            @PathVariable(name = "complaintNo") Long complaintNo,
            @PathVariable(name = "attachmentNo") Long attachmentNo,
            HttpSession session) {
        requireVerified(session, complaintNo);
        try {
            return download(complaintService.getPublicComplaintAttachment(complaintNo, attachmentNo));
        } catch (ComplaintException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @GetMapping("/view/{complaintNo}/answer-attachments/{attachmentNo}")
    public ResponseEntity<Resource> downloadPublicAnswerAttachment(
            @PathVariable(name = "complaintNo") Long complaintNo,
            @PathVariable(name = "attachmentNo") Long attachmentNo,
            HttpSession session) {
        requireVerified(session, complaintNo);
        try {
            return download(complaintService.getPublicComplaintAnswerAttachment(
                    complaintNo,
                    attachmentNo
            ));
        } catch (ComplaintException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    exception.getMessage(),
                    exception
            );
        }
    }

    private void requireVerified(HttpSession session, Long complaintNo) {
        if (!Boolean.TRUE.equals(session.getAttribute(verifiedSessionKey(complaintNo)))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글 비밀번호 확인이 필요합니다.");
        }
    }

    private String verifiedSessionKey(Long complaintNo) {
        return "verifiedComplaint:" + complaintNo;
    }

    private String failureSessionKey(Long complaintNo) {
        return "complaintPasswordFailures:" + complaintNo;
    }

    private String lockSessionKey(Long complaintNo) {
        return "complaintPasswordLockedUntil:" + complaintNo;
    }

    @GetMapping("/new")
    public String complaintForm(
            Authentication authentication,
            @RequestParam(name = "category", defaultValue = "COMPLAINT") String category,
            Model model) {
        employeeService.getRequiredActiveEmployee(authentication.getName());
        String selectedCategory = normalizeFormCategory(category);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("formTitle", switch (selectedCategory) {
            case "PRAISE" -> "칭찬합니다 글쓰기";
            case "LOST" -> "분실물 문의 글쓰기";
            default -> "불편합니다 글쓰기";
        });
        model.addAttribute("formDescription", switch (selectedCategory) {
            case "PRAISE" -> "친절한 기사님과 기분 좋았던 버스 이용 경험을 들려주세요.";
            case "LOST" -> "버스에서 잃어버린 물건과 이용 정보를 자세히 작성해 주세요.";
            default -> "버스 이용 중 겪은 불편 사항이나 개선 의견을 자세히 작성해 주세요.";
        });
        model.addAttribute("formHeroImage", switch (selectedCategory) {
            case "PRAISE" -> "/images/subpages/praise-hero.png";
            case "LOST" -> "/images/subpages/lost-property-hero.png";
            default -> "/images/subpages/inconvenience-hero.png";
        });
        model.addAttribute("titlePlaceholder", switch (selectedCategory) {
            case "PRAISE" -> "예시) 013-1 노선차량 오전 기사님을 칭찬하고 싶어요";
            case "LOST" -> "분실물 제목을 입력해 주세요";
            default -> "예시) 013-1 노선개선일 필요해보여요";
        });
        return "complaint/create";
    }

    private String normalizeFormCategory(String category) {
        if (category == null) return "COMPLAINT";
        return switch (category.trim().toUpperCase()) {
            case "PRAISE" -> "PRAISE";
            case "LOST" -> "LOST";
            default -> "COMPLAINT";
        };
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<ComplaintCreateResponse> createComplaint(
            Authentication authentication,
            @Valid @ModelAttribute ComplaintCreateRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getDefaultMessage())
                    .orElse("입력 내용을 확인해 주세요.");
            return ResponseEntity.badRequest()
                    .body(ComplaintCreateResponse.failure(message));
        }

        try {
            Long complaintNo = complaintService.createComplaint(
                    authentication.getName(), request
            );
            return ResponseEntity.ok(
                    ComplaintCreateResponse.success(complaintNo)
            );
        } catch (ComplaintException exception) {
            return ResponseEntity.badRequest().body(
                    ComplaintCreateResponse.failure(exception.getMessage())
            );
        }
    }

    @GetMapping("/answer-attachments/{attachmentNo}")
    public ResponseEntity<Resource> downloadAnswerAttachment(
            Authentication authentication,
            @PathVariable(name = "attachmentNo") Long attachmentNo) {
        try {
            StoredAttachment attachment = complaintService
                    .getMemberAnswerAttachment(authentication.getName(), attachmentNo);
            return download(attachment);
        } catch (ComplaintException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    exception.getMessage(),
                    exception
            );
        }
    }

    private ResponseEntity<Resource> download(StoredAttachment attachment) {
        FileSystemResource resource = new FileSystemResource(attachment.path());
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String disposition = ContentDisposition.attachment()
                .filename(attachment.originalName(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
