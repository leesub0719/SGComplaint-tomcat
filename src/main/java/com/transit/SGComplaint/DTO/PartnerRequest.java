package com.transit.SGComplaint.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PartnerRequest {

    @NotBlank(message = "협력업체 이름을 입력해 주세요.")
    @Size(max = 100, message = "협력업체 이름은 100자 이내로 입력해 주세요.")
    private String name;

    @NotBlank(message = "협력업체 전화번호를 입력해 주세요.")
    @Size(max = 30, message = "전화번호는 30자 이내로 입력해 주세요.")
    private String phone;

    @Size(max = 500, message = "사이트 주소는 500자 이내로 입력해 주세요.")
    private String site;

    @Size(max = 2000, message = "기타사항은 2,000자 이내로 입력해 주세요.")
    private String notes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
