package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.AdminPartnerItem;
import com.transit.SGComplaint.DTO.PartnerRequest;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.service.AdminPartnerService;
import com.transit.SGComplaint.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/partners")
public class AdminPartnerController {

    private final AdminPartnerService partnerService;
    private final EmployeeService employeeService;

    public AdminPartnerController(
            AdminPartnerService partnerService,
            EmployeeService employeeService) {
        this.partnerService = partnerService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String partners(
            Authentication authentication,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        Page<AdminPartnerItem> partnerPage = partnerService.searchPartners(keyword, page);
        addAdministrator(authentication, model);
        model.addAttribute("activeMenu", "partners");
        model.addAttribute("partnerPage", partnerPage);
        model.addAttribute("partners", partnerPage.getContent());
        model.addAttribute("partnerCount", partnerService.countPartners());
        model.addAttribute("searchCount", partnerPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("partnerRequest", new PartnerRequest());
        return "admin/partners";
    }

    @PostMapping
    public String createPartner(
            @Valid @ModelAttribute PartnerRequest partnerRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getFieldErrors().get(0).getDefaultMessage()
            );
            return "redirect:/admin/partners";
        }
        try {
            Long partnerNo = partnerService.createPartner(partnerRequest);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "협력업체 " + partnerNo + "번을 등록했습니다."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/partners";
    }

    @PostMapping("/{partnerNo}/update")
    public String updatePartner(
            @PathVariable(name = "partnerNo") Long partnerNo,
            @Valid @ModelAttribute PartnerRequest partnerRequest,
            BindingResult bindingResult,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getFieldErrors().get(0).getDefaultMessage()
            );
        } else {
            try {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        partnerService.updatePartner(partnerNo, partnerRequest)
                );
            } catch (IllegalArgumentException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            }
        }
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        return "redirect:/admin/partners";
    }

    @PostMapping("/{partnerNo}/delete")
    public String deletePartner(
            @PathVariable(name = "partnerNo") Long partnerNo,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    partnerService.deletePartner(partnerNo)
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        return "redirect:/admin/partners";
    }

    private void addAdministrator(Authentication authentication, Model model) {
        Employee administrator = employeeService
                .getRequiredActiveEmployee(authentication.getName());
        model.addAttribute("adminName", administrator.getEmpName());
        model.addAttribute("adminRole", administrator.getEmpRole());
        model.addAttribute("isMaster", administrator.isMaster());
    }
}
