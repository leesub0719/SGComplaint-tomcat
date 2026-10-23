package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.service.EmployeeService;
import com.transit.SGComplaint.service.MainBannerService;
import com.transit.SGComplaint.service.NoticeService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final EmployeeService employeeService;
    private final MainBannerService mainBannerService;
    private final NoticeService noticeService;

    public HomeController(
            EmployeeService employeeService,
            MainBannerService mainBannerService,
            NoticeService noticeService) {
        this.employeeService = employeeService;
        this.mainBannerService = mainBannerService;
        this.noticeService = noticeService;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        boolean loggedIn = isLoggedIn(authentication);
        model.addAttribute("isLoggedIn", loggedIn);
        model.addAttribute("isAdmin", false);
        model.addAttribute("mainBanners", mainBannerService.getBanners());
        model.addAttribute("mainNotices", noticeService.getMainNotices());
        model.addAttribute("popupNotices", noticeService.getMainPopupNotices());
        if (loggedIn) {
            model.addAttribute(
                    "memberName",
                    employeeService.getActiveEmployeeName(authentication.getName())
            );
            model.addAttribute(
                    "isAdmin",
                    hasAdminAuthority(authentication)
                            && employeeService.isActiveAdministrator(authentication.getName())
            );
        }
        return "index";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isLoggedIn(authentication)) {
            return "redirect:/";
        }
        return "member/login";
    }

    @GetMapping({
        "/guide",
        "/faq"
    })
    public String comingSoon(Model model) {
        model.addAttribute("title", "페이지 준비 중");
        return "common/coming-soon";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority())
                                || "ROLE_MASTER".equals(authority.getAuthority()));
    }
}
