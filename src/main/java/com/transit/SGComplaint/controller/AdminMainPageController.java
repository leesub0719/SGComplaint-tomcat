package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.service.EmployeeService;
import com.transit.SGComplaint.service.MainBannerService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/main-page")
public class AdminMainPageController {

    private final MainBannerService mainBannerService;
    private final EmployeeService employeeService;

    public AdminMainPageController(
            MainBannerService mainBannerService,
            EmployeeService employeeService) {
        this.mainBannerService = mainBannerService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String mainPageManagement(Authentication authentication, Model model) {
        Employee administrator = employeeService
                .getRequiredActiveEmployee(authentication.getName());
        model.addAttribute("adminName", administrator.getEmpName());
        model.addAttribute("adminRole", administrator.getEmpRole());
        model.addAttribute("isMaster", administrator.isMaster());
        model.addAttribute("activeMenu", "main-page");
        model.addAttribute("banners", mainBannerService.getBanners());
        model.addAttribute("bannerCount", mainBannerService.countBanners());
        model.addAttribute("remainingCount", mainBannerService.remainingCount());
        return "admin/main-page";
    }

    @PostMapping("/banners")
    public String uploadBanners(
            Authentication authentication,
            @RequestParam(name = "bannerImages", required = false)
            List<MultipartFile> bannerImages,
            RedirectAttributes redirectAttributes) {
        try {
            int savedCount = mainBannerService.addBanners(
                    authentication.getName(),
                    bannerImages
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "메인 배너 이미지 " + savedCount + "장을 등록했습니다."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/main-page";
    }

    @PostMapping("/banners/{bannerNo}/delete")
    public String deleteBanner(
            Authentication authentication,
            @PathVariable(name = "bannerNo") Long bannerNo,
            RedirectAttributes redirectAttributes) {
        try {
            mainBannerService.deleteBanner(authentication.getName(), bannerNo);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "메인 배너 이미지를 삭제했습니다."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/main-page";
    }
}
