package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.service.DdokBusGuideImageService;
import com.transit.SGComplaint.service.RouteOperationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RouteOperationController {

    private final RouteOperationService routeOperationService;
    private final DdokBusGuideImageService ddokBusGuideImageService;

    public RouteOperationController(
            RouteOperationService routeOperationService,
            DdokBusGuideImageService ddokBusGuideImageService) {
        this.routeOperationService = routeOperationService;
        this.ddokBusGuideImageService = ddokBusGuideImageService;
    }

    @GetMapping("/route/village-bus")
    public String villageBus(Model model) {
        addRoutePage(model, RouteOperationService.VILLAGE, "마을버스");
        return "route/operation-list";
    }

    @GetMapping("/route/ddokbus")
    public String ddokBus(Model model) {
        addRoutePage(model, RouteOperationService.DDOK, "똑버스");
        return "route/operation-list";
    }

    private void addRoutePage(Model model, String routeType, String title) {
        model.addAttribute("title", title);
        model.addAttribute("activeRouteType", routeType);
        model.addAttribute("routes", routeOperationService.getPublicRoutes(routeType));
        model.addAttribute("ddokBusImages", RouteOperationService.DDOK.equals(routeType)
                ? ddokBusGuideImageService.getImages()
                : java.util.List.of());
        model.addAttribute("pageHeroImage", RouteOperationService.DDOK.equals(routeType)
                ? "/images/subpages/ddokbus-hero.png"
                : "/images/subpages/village-bus-hero.png");
    }
}
