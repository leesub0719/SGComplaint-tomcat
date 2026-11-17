package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.JobPostingItem;
import com.transit.SGComplaint.DTO.JobPostingRequest;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.service.EmployeeService;
import com.transit.SGComplaint.service.RecruitmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/admin/recruitments")
public class AdminRecruitmentController {
    private final RecruitmentService recruitmentService;
    private final EmployeeService employeeService;

    public AdminRecruitmentController(RecruitmentService recruitmentService,
                                      EmployeeService employeeService) {
        this.recruitmentService = recruitmentService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String list(Authentication authentication,
                       @RequestParam(name = "keyword", defaultValue = "") String keyword,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        addAdministrator(authentication, model);
        Page<JobPostingItem> postingPage = recruitmentService.getPostingPage(keyword, page);
        model.addAttribute("activeMenu", "recruitments");
        model.addAttribute("postingPage", postingPage);
        model.addAttribute("postings", postingPage.getContent());
        model.addAttribute("keyword", keyword);
        return "admin/recruitments";
    }

    @GetMapping("/new")
    public String createForm(Authentication authentication, Model model) {
        addAdministrator(authentication, model);
        if (!model.containsAttribute("postingRequest")) {
            model.addAttribute("postingRequest", new JobPostingRequest());
        }
        preparePostingForm(model, false, null);
        return "admin/recruitment-form";
    }

    @PostMapping
    public String create(Authentication authentication,
                         @Valid @ModelAttribute("postingRequest") JobPostingRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addAdministrator(authentication, model);
            preparePostingForm(model, false, null);
            return "admin/recruitment-form";
        }
        Long postingNo = recruitmentService.createPosting(authentication.getName(), request);
        redirectAttributes.addFlashAttribute("successMessage", "채용공고 " + postingNo + "번을 등록했습니다.");
        return "redirect:/admin/recruitments";
    }

    @GetMapping("/{postingNo}/edit")
    public String editForm(Authentication authentication,
                           @PathVariable(name = "postingNo") Long postingNo,
                           Model model) {
        addAdministrator(authentication, model);
        model.addAttribute("postingRequest", recruitmentService.getPostingRequest(postingNo));
        preparePostingForm(model, true, postingNo);
        return "admin/recruitment-form";
    }

    @PostMapping("/{postingNo}")
    public String update(Authentication authentication,
                         @PathVariable(name = "postingNo") Long postingNo,
                         @Valid @ModelAttribute("postingRequest") JobPostingRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addAdministrator(authentication, model);
            preparePostingForm(model, true, postingNo);
            return "admin/recruitment-form";
        }
        recruitmentService.updatePosting(postingNo, request);
        redirectAttributes.addFlashAttribute("successMessage", "채용공고 " + postingNo + "번을 수정했습니다.");
        return "redirect:/admin/recruitments";
    }

    @PostMapping("/{postingNo}/delete")
    public String delete(@PathVariable(name = "postingNo") Long postingNo,
                         RedirectAttributes redirectAttributes) {
        try {
            recruitmentService.deletePosting(postingNo);
            redirectAttributes.addFlashAttribute("successMessage", "채용공고 " + postingNo + "번을 삭제했습니다.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/recruitments";
    }

    @PostMapping("/{postingNo}/status")
    public String changeStatus(@PathVariable(name = "postingNo") Long postingNo,
                               @RequestParam(name = "active") boolean active,
                               RedirectAttributes redirectAttributes) {
        recruitmentService.changePostingStatus(postingNo, active);
        redirectAttributes.addFlashAttribute("successMessage", active ? "채용공고 접수를 재개했습니다." : "채용공고 접수를 마감했습니다.");
        return "redirect:/admin/recruitments";
    }

    @GetMapping("/{postingNo}/applications")
    public String applications(Authentication authentication,
                               @PathVariable(name = "postingNo") Long postingNo,
                               Model model) {
        addAdministrator(authentication, model);
        model.addAttribute("activeMenu", "recruitments");
        model.addAttribute("posting", recruitmentService.getPostingDetail(postingNo));
        model.addAttribute("applications", recruitmentService.getApplications(postingNo));
        return "admin/recruitment-applications";
    }

    @GetMapping("/applications/{applicationNo}/files.zip")
    public ResponseEntity<StreamingResponseBody> downloadArchive(
            @PathVariable(name = "applicationNo") Long applicationNo) {
        String filename = recruitmentService.archiveFilename(applicationNo);
        StreamingResponseBody body = output -> recruitmentService.writeApplicationArchive(applicationNo, output);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(body);
    }

    private void addAdministrator(Authentication authentication, Model model) {
        Employee administrator = employeeService.getRequiredActiveEmployee(authentication.getName());
        model.addAttribute("adminName", administrator.getEmpName());
        model.addAttribute("adminRole", administrator.getEmpRole());
        model.addAttribute("isMaster", administrator.isMaster());
    }

    private void preparePostingForm(Model model, boolean editMode, Long postingNo) {
        model.addAttribute("activeMenu", "recruitments");
        model.addAttribute("documentOptions", recruitmentService.getDocumentOptions());
        model.addAttribute("editMode", editMode);
        model.addAttribute("postingNo", postingNo);
        model.addAttribute("formAction", editMode
                ? "/admin/recruitments/" + postingNo
                : "/admin/recruitments");
        model.addAttribute("formTitle", editMode ? "채용공고 수정" : "채용공고 등록");
        model.addAttribute("formDescription", editMode
                ? "등록된 공고의 제목, 상세내용, 접수 기간과 공개 상태를 수정합니다."
                : "지원자가 확인할 제목, 상세내용과 접수 기간을 입력합니다.");
        model.addAttribute("submitLabel", editMode ? "수정 내용 저장" : "채용공고 등록");
    }
}
