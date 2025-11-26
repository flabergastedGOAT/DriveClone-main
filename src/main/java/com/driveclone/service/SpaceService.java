package com.driveclone.service;

import com.driveclone.model.Activity;
import com.driveclone.model.Space;
import com.driveclone.model.SpaceFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class SpaceService {
    private static final Logger logger = LoggerFactory.getLogger(SpaceService.class);
    private final SqliteMetadataService metadataService;
    private final LocalFileStorageService storageService;

    public SpaceService() {
        this.metadataService = new SqliteMetadataService();
        this.storageService = new LocalFileStorageService();
    }

    public String createSpace(String name, String description, String adminId, String adminEmail) {
        Space space = new Space(name, description, adminId, adminEmail);
        // Generate a unique ID for the space
        space.setId(java.util.UUID.randomUUID().toString());
        String spaceId = metadataService.createSpace(space);
        
        // Log activity
        metadataService.logActivity(spaceId, adminEmail, "created space", name);
        
        return spaceId;
    }

    public Optional<Space> getSpace(String spaceId) {
        return metadataService.getSpace(spaceId);
    }

    public List<Space> getSpacesForUser(String userEmail) {
        return metadataService.getSpacesForUser(userEmail);
    }

    public void updateSpace(Space space) {
        metadataService.updateSpace(space);
    }

    public void deleteSpace(String spaceId) {
        // Delete all files in the space first
        storageService.deleteAllFilesInSpace(spaceId);
        // Then delete the space
        metadataService.deleteSpace(spaceId);
    }

    public void addMemberToSpace(String spaceId, String memberEmail, String actorEmail) {
        metadataService.addMemberToSpace(spaceId, memberEmail);
        metadataService.logActivity(spaceId, actorEmail, "added member", memberEmail);
    }

    public void removeMemberFromSpace(String spaceId, String memberEmail, String actorEmail) {
        metadataService.removeMemberFromSpace(spaceId, memberEmail);
        metadataService.logActivity(spaceId, actorEmail, "removed member", memberEmail);
    }
        
    public void updateMemberRole(String spaceId, String memberEmail, String role, String actorEmail) {
        metadataService.updateMemberRole(spaceId, memberEmail, role);
        metadataService.logActivity(spaceId, actorEmail, "updated member role", memberEmail + " -> " + role.toUpperCase());
    }

    public boolean isUserMemberOfSpace(String spaceId, String userEmail) {
        return metadataService.isUserMemberOfSpace(spaceId, userEmail);
    }

    public boolean isUserAdminOfSpace(String spaceId, String userEmail) {
        return metadataService.isUserAdminOfSpace(spaceId, userEmail);
    }

    // File operations
    public String uploadFile(InputStream inputStream, String spaceId, String originalFilename, 
                           String contentType, long size, String uploaderId, String uploaderEmail) {
        // Check if user is member of space
        if (!isUserMemberOfSpace(spaceId, uploaderEmail)) {
            throw new RuntimeException("User is not a member of this space");
        }

        // Upload to Local Storage
        String storagePath = storageService.uploadFile(inputStream, spaceId, originalFilename, contentType);
        
        // Save metadata to SQLite
        SpaceFile file = new SpaceFile(spaceId, originalFilename, storagePath, contentType, size, uploaderId, uploaderEmail);
        // Generate a unique ID for the file
        file.setId(java.util.UUID.randomUUID().toString());
        String fileId = metadataService.createFile(file);
        
        // Log activity
        metadataService.logActivity(spaceId, uploaderEmail, "uploaded file", originalFilename);
        
        return fileId;
    }

    public List<SpaceFile> getFilesForSpace(String spaceId) {
        return metadataService.getFilesForSpace(spaceId);
    }

    public Optional<SpaceFile> getFile(String fileId) {
        return metadataService.getFile(fileId);
    }

    public void deleteFile(String fileId, String userEmail) {
        Optional<SpaceFile> fileOpt = metadataService.getFile(fileId);
        if (fileOpt.isEmpty()) {
            throw new RuntimeException("File not found");
        }

        SpaceFile file = fileOpt.get();
        String spaceId = file.getSpaceId();

        // Check permissions: user must be admin or the file uploader
        if (!isUserAdminOfSpace(spaceId, userEmail) && !file.getUploaderEmail().equals(userEmail)) {
            throw new RuntimeException("You don't have permission to delete this file");
        }

        // Delete from storage
        storageService.deleteFile(file.getStoragePath());
        
        // Delete from SQLite
        metadataService.deleteFile(fileId);
        
        // Log activity
        metadataService.logActivity(spaceId, userEmail, "deleted file", file.getOriginalFilename());
    }

    public InputStream downloadFile(String fileId, String userEmail) {
        Optional<SpaceFile> fileOpt = metadataService.getFile(fileId);
        if (fileOpt.isEmpty()) {
            throw new RuntimeException("File not found");
        }

        SpaceFile file = fileOpt.get();
        String spaceId = file.getSpaceId();

        // Check if user is member of space
        if (!isUserMemberOfSpace(spaceId, userEmail)) {
            throw new RuntimeException("You don't have access to this file");
        }

        return storageService.downloadFile(file.getStoragePath());
    }

    public List<Activity> getActivityLog(String spaceId) {
        return metadataService.getActivityLog(spaceId);
    }
}
