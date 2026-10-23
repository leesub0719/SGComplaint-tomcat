package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.MemberPasswordConfirmRequest;
import com.transit.SGComplaint.DTO.MemberProfileUpdateRequest;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.service.ComplaintService;
import com.transit.SGComplaint.service.EmployeeService;
import com.transit.SGComplaint.service.PhoneVerificationException;
import com.transit.SGComplaint.service.ProfileVerificationSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/mypage")
public class MyPageController {

    private final EmployeeService employeeService;
    private final ComplaintService complaintService;
    private final ProfileVerificationSession verificationSession;

    public MyPageController(
            EmployeeService employeeService,
            ComplaintService complaintService,
            ProfileVerificationSession verificationSession) {
        this.employeeService = employeeService;
        this.complaintService = complaintService;
        this.verificationSession = verificationSession;
    }

    @GetMapping
    public String myPage() {
        return "redirect:/mypage/profile";
    }

    @GetMapping("/profile")
    public String profile(
            Authentication authentication,
            HttpSession session,
            Model model) {
        Employee employee = employeeService.getRequiredActiveEmployee(
                authentication.getName());
        addCommonModel(model, employee, "profile");

        if (!isProfileVerified(session)) {
            model.addAttribute("passwordForm", new MemberPasswordConfirmRequest());
            return "mypage/password-confirm";
        }

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute(
                    "profileForm",
                    employeeService.getProfileUpdateForm(authentication.getName()));
        }
        return "mypage/profile";
    }

    @PostMapping("/profile/confirm-password")
    public String confirmPassword(
            Authentication authentication,
            @Valid @ModelAttribute("passwordForm") MemberPasswordConfirmRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {
        Employee employee = employeeService.getRequiredActiveEmployee(
                authentication.getName());
        addCommonModel(model, employee, "profile");

        if (!bindingResult.hasErrors()
                && !employeeService.matchesCurrentPassword(
                        authentication.getName(), request.getCurrentPassword())) {
            bindingResult.rejectValue(
                    "currentPassword",
                    "password.mismatch",
                    "현재 비밀번호가 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            return "mypage/password-confirm";
        }

        verificationSession.markVerified(session);
        return "redirect:/mypage/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute("profileForm") MemberProfileUpdateRequest request,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!isProfileVerified(session)) {
            return "redirect:/mypage/profile";
        }
        if (!request.passwordMatches()) {
            bindingResult.rejectValue(
                    "newPasswordConfirm",
                    "password.mismatch",
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            addCommonModel(
                    model,
                    employeeService.getRequiredActiveEmployee(authentication.getName()),
                    "profile");
            return "mypage/profile";
        }

        try {
            employeeService.updateProfile(authentication.getName(), request);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "회원정보가 저장되었습니다.");
            return "redirect:/mypage/profile";
        } catch (PhoneVerificationException exception) {
            bindingResult.rejectValue(
                    "phoneVerificationToken",
                    "phone.verification.invalid",
                    exception.getMessage());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("profile.invalid", exception.getMessage());
        }

        addCommonModel(
                model,
                employeeService.getRequiredActiveEmployee(authentication.getName()),
                "profile");
        return "mypage/profile";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            Authentication authentication,
            HttpServletRequest servletRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (!isProfileVerified(session)) {
            return "redirect:/mypage/profile";
        }
        try {
            employeeService.withdraw(authentication.getName());
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/mypage/profile";
        }

        SecurityContextHolder.clearContext();
        servletRequest.getSession(false).invalidate();
        return "redirect:/?withdrawn";
    }

    @GetMapping("/inquiries")
    public String inquiries(
            Authentication authentication,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        LocalDate today = LocalDate.now();
        LocalDate normalizedEnd = endDate == null ? today : endDate;
        LocalDate normalizedStart = startDate == null
                ? normalizedEnd.minusYears(1) : startDate;
        if (normalizedStart.isAfter(normalizedEnd)) {
            LocalDate swap = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = swap;
        }

        Employee employee = employeeService.getRequiredActiveEmployee(
                authentication.getName());
        addCommonModel(model, employee, "inquiries");
        model.addAttribute("startDate", normalizedStart);
        model.addAttribute("endDate", normalizedEnd);
        var inquiryPage = complaintService.getMemberComplaints(
                employee.getEmpNo(), normalizedStart, normalizedEnd, page);
        model.addAttribute("inquiryPage", inquiryPage);
        model.addAttribute("inquiries", inquiryPage.getContent());
        return "mypage/inquiries";
    }

    private boolean isProfileVerified(HttpSession session) {
        return verificationSession.isVerified(session);
    }

    private void addCommonModel(
            Model model,
            Employee employee,
            String activeMenu) {
        model.addAttribute("employee", employee);
        model.addAttribute("activeMyPageMenu", activeMenu);
    }
}
