package com.driveclone.model;

import java.time.LocalDateTime;

public class SpaceMember {
    private String email;
    private String role;
    private LocalDateTime addedAt;
    private boolean owner;

    public SpaceMember() {}

    public SpaceMember(String email, String role, LocalDateTime addedAt, boolean owner) {
        this.email = email;
        this.role = role;
        this.addedAt = addedAt;
        this.owner = owner;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }
}

