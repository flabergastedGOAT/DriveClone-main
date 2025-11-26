package com.driveclone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class LocalFileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);
    private final String storageBasePath;

    public LocalFileStorageService() {
        this.storageBasePath = "uploads";
        createStorageDirectory();
        logger.info("Local File Storage Service initialized with path: {}", storageBasePath);
    }

    private void createStorageDirectory() {
        try {
            Path storagePath = Paths.get(storageBasePath);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                logger.info("Created storage directory: {}", storageBasePath);
            }
        } catch (IOException e) {
            logger.error("Failed to create storage directory", e);
            throw new RuntimeException("Failed to create storage directory", e);
        }
    }

    public String uploadFile(InputStream inputStream, String spaceId, String originalFilename, String contentType) {
        try {
            // Generate unique filename to avoid conflicts
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            String storagePath = "spaces/" + spaceId + "/files/" + uniqueFilename;
            
            // Create directory structure
            Path fullPath = Paths.get(storageBasePath, storagePath);
            Files.createDirectories(fullPath.getParent());
            
            // Write file to local storage
            try (FileOutputStream fos = new FileOutputStream(fullPath.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            logger.info("Uploaded file to local storage: {}", storagePath);
            return storagePath;
        } catch (Exception e) {
            logger.error("Error uploading file to local storage", e);
            throw new RuntimeException("Failed to upload file to local storage", e);
        }
    }

    public InputStream downloadFile(String storagePath) {
        try {
            Path fullPath = Paths.get(storageBasePath, storagePath);
            if (!Files.exists(fullPath)) {
                throw new RuntimeException("File not found: " + storagePath);
            }
            
            return Files.newInputStream(fullPath);
        } catch (Exception e) {
            logger.error("Error downloading file from local storage: {}", storagePath, e);
            throw new RuntimeException("Failed to download file from local storage", e);
        }
    }

    public void deleteFile(String storagePath) {
        try {
            Path fullPath = Paths.get(storageBasePath, storagePath);
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                logger.info("Deleted file from local storage: {}", storagePath);
            } else {
                logger.warn("File not found for deletion: {}", storagePath);
            }
        } catch (Exception e) {
            logger.error("Error deleting file from local storage: {}", storagePath, e);
            throw new RuntimeException("Failed to delete file from local storage", e);
        }
    }

    public void deleteAllFilesInSpace(String spaceId) {
        try {
            Path spacePath = Paths.get(storageBasePath, "spaces", spaceId);
            if (Files.exists(spacePath)) {
                Files.walk(spacePath)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.info("Deleted: {}", path);
                        } catch (IOException e) {
                            logger.error("Error deleting: {}", path, e);
                        }
                    });
                logger.info("Deleted all files in space: {}", spaceId);
            }
        } catch (Exception e) {
            logger.error("Error deleting files in space: {}", spaceId, e);
            throw new RuntimeException("Failed to delete files in space", e);
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
}
