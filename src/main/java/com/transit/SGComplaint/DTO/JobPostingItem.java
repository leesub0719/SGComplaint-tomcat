package com.transit.SGComplaint.DTO;

public record JobPostingItem(Long postingNo, String title, String deadline,
                             String statusLabel, String statusCssClass,
                             long applicationCount, String createdDate) { }
