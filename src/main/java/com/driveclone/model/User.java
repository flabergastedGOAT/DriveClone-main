package com.driveclone.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String email;
    private String name;
    private String firebaseUid;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public User() {}

    public User(String email, String name, String firebaseUid) {
        this.email = email;
        this.name = name;
        this.firebaseUid = firebaseUid;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
