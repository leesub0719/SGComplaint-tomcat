package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public class ComplaintCreateRequest {

    @NotBlank(message = "민원 분류를 선택해 주세요.")
    @Pattern(regexp = "PRAISE|COMPLAINT|LOST", message = "올바른 민원 분류를 선택해 주세요.")
    private String category;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해 주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(max = 10000, message = "서식을 포함한 내용이 너무 깁니다.")
    private String content;

    @NotBlank(message = "게시글 비밀번호를 입력해 주세요.")
    @Size(min = 4, max = 20, message = "게시글 비밀번호는 4~20자로 입력해 주세요.")
    private String postPassword;

    private List<MultipartFile> attachments = new ArrayList<>();

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPostPassword() { return postPassword; }
    public void setPostPassword(String postPassword) { this.postPassword = postPassword; }
    public List<MultipartFile> getAttachments() { return attachments; }
    public void setAttachments(List<MultipartFile> attachments) {
        this.attachments = attachments == null ? new ArrayList<>() : attachments;
    }
}
