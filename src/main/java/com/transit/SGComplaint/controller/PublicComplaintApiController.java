package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.PublicComplaintItem;
import com.transit.SGComplaint.DTO.PublicComplaintPageResponse;
import com.transit.SGComplaint.service.ComplaintService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/complaints")
public class PublicComplaintApiController {

    private final ComplaintService complaintService;

    public PublicComplaintApiController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public PublicComplaintPageResponse getComplaints(
            @RequestParam(name = "category", defaultValue = "ALL") String category,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        Page<PublicComplaintItem> result = complaintService.getPublicComplaints(category, keyword, page);
        return new PublicComplaintPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.isFirst(),
                result.isLast()
        );
    }
}
