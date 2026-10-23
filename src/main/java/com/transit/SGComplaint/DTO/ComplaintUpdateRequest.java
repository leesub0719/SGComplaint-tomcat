package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ComplaintUpdateRequest {

    @NotBlank(message = "민원 분류를 선택해 주세요.")
    @Pattern(
            regexp = "PRAISE|COMPLAINT|LOST",
            message = "올바른 민원 분류를 선택해 주세요.")
    private String category;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해 주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(max = 2000, message = "내용은 2000자 이내로 입력해 주세요.")
    private String content;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
