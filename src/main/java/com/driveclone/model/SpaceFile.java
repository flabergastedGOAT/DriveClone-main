package com.driveclone.model;

import java.time.LocalDateTime;

public class SpaceFile {
    private String id;
    private String spaceId;
    private String originalFilename;
    private String storagePath;
    private String contentType;
    private long size;
    private String uploaderId;
    private String uploaderEmail;
    private LocalDateTime uploadedAt;

    public SpaceFile() {}

    public SpaceFile(String spaceId, String originalFilename, String storagePath, 
                    String contentType, long size, String uploaderId, String uploaderEmail) {
        this.spaceId = spaceId;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.size = size;
        this.uploaderId = uploaderId;
        this.uploaderEmail = uploaderEmail;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getUploaderEmail() {
        return uploaderEmail;
    }

    public void setUploaderEmail(String uploaderEmail) {
        this.uploaderEmail = uploaderEmail;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
