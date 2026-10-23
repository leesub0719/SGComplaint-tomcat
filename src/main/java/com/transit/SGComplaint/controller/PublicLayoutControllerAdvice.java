package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.service.EmployeeService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class PublicLayoutControllerAdvice {

    private final EmployeeService employeeService;

    public PublicLayoutControllerAdvice(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @ModelAttribute
    public void addPublicLayoutAttributes(Authentication authentication, Model model) {
        boolean loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("isLoggedIn", loggedIn);
        model.addAttribute("isAdmin", false);
        if (!loggedIn) return;

        model.addAttribute("memberName", employeeService.getActiveEmployeeName(authentication.getName()));
        boolean hasAdminAuthority = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_MASTER".equals(authority.getAuthority()));
        model.addAttribute("isAdmin", hasAdminAuthority
                && employeeService.isActiveAdministrator(authentication.getName()));
    }
}
