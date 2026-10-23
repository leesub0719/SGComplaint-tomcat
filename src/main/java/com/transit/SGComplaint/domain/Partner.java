package com.transit.SGComplaint.domain;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
public class Partner {
    private Long partnerNo;
    private String name;
    private String phone;
    private String site;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Partner() {
    }

    public static Partner create(String name, String phone, String site, String notes) {
        Partner partner = new Partner();
        partner.changeInformation(name, phone, site, notes);
        partner.status = "Y";
        return partner;
    }

    public void changeInformation(String name, String phone, String site, String notes) {
        this.name = name.trim();
        this.phone = phone.trim();
        this.site = site;
        this.notes = notes;
    }

    public void deactivate() {
        this.status = "N";
    }
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        createdAt = now;
        updatedAt = now;
    }
    private void onUpdate() {
        updatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getPartnerNo() { return partnerNo; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getSite() { return site; }
    public String getNotes() { return notes; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
