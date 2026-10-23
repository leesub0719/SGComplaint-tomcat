package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.RouteOperationRequest;
import com.transit.SGComplaint.domain.Employee;
import com.transit.SGComplaint.service.DdokBusGuideImageService;
import com.transit.SGComplaint.service.EmployeeService;
import com.transit.SGComplaint.service.RouteOperationService;
import jakarta.validation.Valid;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/routes")
public class AdminRouteOperationController {

    private final RouteOperationService routeOperationService;
    private final DdokBusGuideImageService ddokBusGuideImageService;
    private final EmployeeService employeeService;

    public AdminRouteOperationController(
            RouteOperationService routeOperationService,
            DdokBusGuideImageService ddokBusGuideImageService,
            EmployeeService employeeService) {
        this.routeOperationService = routeOperationService;
        this.ddokBusGuideImageService = ddokBusGuideImageService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String routes(Authentication authentication, Model model) {
        addAdministrator(authentication, model);
        model.addAttribute("activeMenu", "routes");
        model.addAttribute("routes", routeOperationService.getAdminRoutes());
        model.addAttribute("routeCount", routeOperationService.countRoutes());
        model.addAttribute("ddokBusImages", ddokBusGuideImageService.getImages());
        model.addAttribute("ddokBusImageCount", ddokBusGuideImageService.countImages());
        model.addAttribute("routeRequest", new RouteOperationRequest());
        return "admin/routes";
    }

    @PostMapping
    public String createRoute(
            @ModelAttribute RouteOperationRequest routeRequest,
            @RequestParam(name = "guideImage", required = false)
            MultipartFile guideImage,
            RedirectAttributes redirectAttributes) {
        try {
            if (RouteOperationService.DDOK.equals(routeRequest.getRouteType())) {
                Long imageNo = ddokBusGuideImageService.addImage(guideImage);
                redirectAttributes.addFlashAttribute(
                        "successMessage", "똑버스 안내 이미지 " + imageNo + "번을 등록했습니다.");
                return "redirect:/admin/routes";
            }
            Long routeNo = routeOperationService.createRoute(routeRequest);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "운행안내 " + routeNo + "번을 등록했습니다.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/routes";
    }

    @PostMapping("/ddok-images/{imageNo}/delete")
    public String deleteDdokBusImage(
            @PathVariable(name = "imageNo") Long imageNo,
            RedirectAttributes redirectAttributes) {
        try {
            ddokBusGuideImageService.deleteImage(imageNo);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "똑버스 안내 이미지를 삭제했습니다.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/routes";
    }

    @PostMapping("/{routeNo}/update")
    public String updateRoute(
            @PathVariable(name = "routeNo") Long routeNo,
            @Valid @ModelAttribute RouteOperationRequest routeRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getFieldErrors().get(0).getDefaultMessage());
        } else {
            try {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        routeOperationService.updateRoute(routeNo, routeRequest));
            } catch (IllegalArgumentException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            }
        }
        return "redirect:/admin/routes";
    }

    @PostMapping("/{routeNo}/delete")
    public String deleteRoute(
            Authentication authentication,
            @PathVariable(name = "routeNo") Long routeNo,
            RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute(
                    "successMessage", routeOperationService.deleteRoute(authentication.getName(), routeNo));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/routes";
    }

    private void addAdministrator(Authentication authentication, Model model) {
        Employee administrator = employeeService
                .getRequiredActiveEmployee(authentication.getName());
        model.addAttribute("adminName", administrator.getEmpName());
        model.addAttribute("adminRole", administrator.getEmpRole());
        model.addAttribute("isMaster", administrator.isMaster());
    }
}
