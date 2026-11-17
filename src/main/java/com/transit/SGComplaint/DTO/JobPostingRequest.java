package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JobPostingRequest {
    @NotBlank(message = "채용공고 제목을 입력해 주세요.")
    @Size(max = 150, message = "제목은 150자 이내로 입력해 주세요.")
    private String title;

    @NotBlank(message = "채용공고 내용을 입력해 주세요.")
    @Size(max = 10000, message = "내용은 10,000자 이내로 입력해 주세요.")
    private String content;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    private boolean active = true;
    private boolean alwaysOpen;
    private List<String> documentTypes = new ArrayList<>(List.of(
            "RESUME", "DRIVER_LICENSE", "BUS_DRIVER_LICENSE",
            "APTITUDE_RESULT", "TRAINING_CERTIFICATE",
            "DRIVING_CAREER", "RESIDENT_REGISTRATION",
            "MEDICAL_CERTIFICATE", "CAREER_CERTIFICATE"));

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isAlwaysOpen() { return alwaysOpen; }
    public void setAlwaysOpen(boolean alwaysOpen) { this.alwaysOpen = alwaysOpen; }
    public List<String> getDocumentTypes() { return documentTypes; }
    public void setDocumentTypes(List<String> documentTypes) {
        this.documentTypes = documentTypes == null ? new ArrayList<>() : new ArrayList<>(documentTypes);
    }

    @AssertTrue(message = "상시 모집이 아니면 오늘 이후의 지원 마감일을 선택해 주세요.")
    public boolean isDeadlineConfigurationValid() {
        return alwaysOpen || (deadline != null && !deadline.isBefore(LocalDate.now()));
    }
}
