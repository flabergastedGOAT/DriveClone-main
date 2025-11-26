package com.driveclone.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Space {
    private String id;
    private String name;
    private String description;
    private String adminId;
    private String adminEmail;
    private LocalDateTime createdAt;
    private List<String> memberEmails;
    private List<SpaceMember> members = new ArrayList<>();

    public Space() {}

    public Space(String name, String description, String adminId, String adminEmail) {
        this.name = name;
        this.description = description;
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getMemberEmails() {
        return memberEmails;
    }

    public void setMemberEmails(List<String> memberEmails) {
        this.memberEmails = memberEmails;
    }

    public List<SpaceMember> getMembers() {
        return members;
    }

    public void setMembers(List<SpaceMember> members) {
        this.members = members;
    }
}
