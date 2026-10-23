package com.transit.SGComplaint.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class PublicMenuController {

    private static final Map<String, String> MENU_TITLES = Map.ofEntries(
            Map.entry("/company/greeting", "인사말"),
            Map.entry("/company/history", "회사연혁"),
            Map.entry("/company/location", "오시는길"),
            Map.entry("/company/organization", "조직도"),
            Map.entry("/route/village-bus", "마을버스"),
            Map.entry("/route/ddokbus", "똑버스"),
            Map.entry("/recruit/notices", "채용공고")
    );
    private static final Map<String, String> MENU_HERO_IMAGES = Map.ofEntries(
            Map.entry("/company/greeting", "/images/subpages/company-greeting-hero.png"),
            Map.entry("/company/history", "/images/subpages/company-history-hero.png"),
            Map.entry("/company/location", "/images/subpages/company-location-hero.png"),
            Map.entry("/company/organization", "/images/subpages/company-organization-hero.png"),
            Map.entry("/route/village-bus", "/images/subpages/village-bus-hero.png"),
            Map.entry("/route/ddokbus", "/images/subpages/ddokbus-hero.png"),
            Map.entry("/recruit/notices", "/images/subpages/recruit-notices-hero.png")
    );

    @GetMapping({
            "/company/history",
            "/company/location",
            "/company/organization",
            "/recruit/notices"
    })
    public String menuPage(HttpServletRequest request, Model model) {
        String title = MENU_TITLES.getOrDefault(request.getServletPath(), "페이지 준비 중");
        model.addAttribute("title", title);
        model.addAttribute("description", title + " 페이지는 다음 단계에서 기능을 연결할 예정입니다.");
        model.addAttribute("pageHeroImage", MENU_HERO_IMAGES.getOrDefault(
                request.getServletPath(),
                "/images/subpages/coming-soon-hero.png"
        ));
        return "common/coming-soon";
    }

    @GetMapping("/company/greeting")
    public String greetingPage() {
        return "company/greeting";
    }

    @GetMapping("/customer/praise")
    public String praiseBoard() {
        return "redirect:/complaints?category=PRAISE";
    }

    @GetMapping("/customer/notices")
    public String noticeBoard() {
        return "redirect:/notices";
    }

    @GetMapping("/customer/complaints")
    public String complaintBoard() {
        return "redirect:/complaints?category=COMPLAINT";
    }

    @GetMapping("/customer/lost-found")
    public String lostFoundBoard() {
        return "redirect:/complaints?category=LOST";
    }
}
