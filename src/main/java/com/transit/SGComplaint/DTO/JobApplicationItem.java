package com.transit.SGComplaint.DTO;

public record JobApplicationItem(Long applicationNo, String applicantName,
                                 String phone, String email, String applicantTypeLabel,
                                 String memo, String appliedDateTime, int fileCount) { }
