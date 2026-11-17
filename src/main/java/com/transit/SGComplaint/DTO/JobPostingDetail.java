package com.transit.SGComplaint.DTO;

import java.util.List;

public record JobPostingDetail(Long postingNo, String title, String content,
                               String deadline, String statusLabel,
                               String statusCssClass, boolean open,
                               List<String> documentTypes) { }
