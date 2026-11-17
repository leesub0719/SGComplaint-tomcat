package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.JobApplicationRequest;
import com.transit.SGComplaint.DTO.JobPostingDetail;
import com.transit.SGComplaint.DTO.JobPostingItem;
import com.transit.SGComplaint.service.ApplicationFileValidationException;
import com.transit.SGComplaint.service.RecruitmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
@RequestMapping("/recruit/notices")
public class RecruitmentController {
    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @GetMapping
    public String list(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        Page<JobPostingItem> postingPage = recruitmentService.getPostingPage(keyword, page);
        model.addAttribute("postingPage", postingPage);
        model.addAttribute("postings", postingPage.getContent());
        model.addAttribute("keyword", keyword);
        return "recruit/list";
    }

    @GetMapping("/{postingNo}")
    public String detail(@PathVariable(name = "postingNo") Long postingNo, Model model) {
        model.addAttribute("posting", requiredPosting(postingNo));
        return "recruit/detail";
    }

    @GetMapping("/{postingNo}/apply")
    public String applyForm(@PathVariable(name = "postingNo") Long postingNo, Model model) {
        JobPostingDetail posting = requiredPosting(postingNo);
        if (!posting.open()) throw new ResponseStatusException(HttpStatus.CONFLICT, "접수가 마감된 채용공고입니다.");
        model.addAttribute("posting", posting);
        if (!model.containsAttribute("applicationRequest")) {
            model.addAttribute("applicationRequest", new JobApplicationRequest());
        }
        return "recruit/apply";
    }

    @PostMapping("/{postingNo}/apply")
    public String apply(@PathVariable(name = "postingNo") Long postingNo,
                        @Valid @ModelAttribute("applicationRequest") JobApplicationRequest request,
                        BindingResult bindingResult,
                        Model model) {
        JobPostingDetail posting = requiredPosting(postingNo);
        if (bindingResult.hasErrors()) {
            model.addAttribute("posting", posting);
            return "recruit/apply";
        }
        try {
            recruitmentService.apply(postingNo, request);
            return "redirect:/recruit/notices/" + postingNo + "?applied";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("application", exception.getMessage());
            model.addAttribute("posting", posting);
            return "recruit/apply";
        }
    }

    @PostMapping(value = "/{postingNo}/apply", headers = "X-Requested-With=XMLHttpRequest")
    public ResponseEntity<Map<String, String>> applyAsync(
            @PathVariable(name = "postingNo") Long postingNo,
            @Valid @ModelAttribute("applicationRequest") JobApplicationRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            var error = bindingResult.getFieldErrors().stream().findFirst();
            String field = error.map(item -> item.getField()).orElse("");
            String message = error.map(item -> item.getDefaultMessage())
                    .orElseGet(() -> bindingResult.getAllErrors().get(0).getDefaultMessage());
            return validationFailure(field, message);
        }

        try {
            recruitmentService.apply(postingNo, request);
            return ResponseEntity.ok(Map.of(
                    "redirectUrl", httpRequest.getContextPath()
                            + "/recruit/notices/" + postingNo + "?applied"));
        } catch (ApplicationFileValidationException exception) {
            return validationFailure(exception.getFieldName(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return validationFailure("", exception.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> validationFailure(String field, String message) {
        return ResponseEntity.badRequest().body(Map.of(
                "field", field == null ? "" : field,
                "message", message == null ? "입력 내용을 확인해 주세요." : message));
    }

    private JobPostingDetail requiredPosting(Long postingNo) {
        try {
            return recruitmentService.getPostingDetail(postingNo);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
