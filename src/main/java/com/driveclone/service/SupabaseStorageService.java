package com.driveclone.service;

import com.driveclone.config.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class SupabaseStorageService {
    private static final Logger logger = LoggerFactory.getLogger(SupabaseStorageService.class);
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    private final String supabaseServiceKey;
    private final Gson gson = new Gson();

    public SupabaseStorageService() {
        Config config = Config.getInstance();
        this.supabaseUrl = config.getSupabaseUrl();
        this.supabaseAnonKey = config.getSupabaseAnonKey();
        
        String serviceKey = config.getSupabaseServiceRoleKey();
        if (serviceKey == null || serviceKey.contains("example")) {
            logger.warn("Supabase service role key not configured properly. File operations may fail.");
            this.supabaseServiceKey = this.supabaseAnonKey; // Fallback to anon key
        } else {
            this.supabaseServiceKey = serviceKey;
        }
        
        if (supabaseUrl == null || supabaseAnonKey == null) {
            throw new RuntimeException("Supabase configuration missing. Please set SUPABASE_URL and SUPABASE_ANON_KEY environment variables.");
        }
        
        logger.info("Supabase Storage Service initialized with URL: {}", supabaseUrl);
    }

    public String uploadFile(InputStream inputStream, String spaceId, String originalFilename, String contentType) {
        try {
            // Generate unique filename to avoid conflicts
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            String storagePath = "spaces/" + spaceId + "/files/" + uniqueFilename;
            
            // Convert InputStream to byte array
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] fileBytes = buffer.toByteArray();
            
            // Upload to Supabase Storage
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + storagePath;
            URL url = new URL(uploadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + supabaseServiceKey);
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("Content-Length", String.valueOf(fileBytes.length));
            connection.setDoOutput(true);
            
            // Write file data
            try (OutputStream os = connection.getOutputStream()) {
                os.write(fileBytes);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Uploaded file to Supabase: {}", storagePath);
                return storagePath;
            } else {
                String errorMessage = readErrorResponse(connection);
                logger.error("Failed to upload file to Supabase. Response code: {}, Error: {}", responseCode, errorMessage);
                throw new RuntimeException("Failed to upload file to Supabase: " + errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error uploading file to Supabase", e);
            throw new RuntimeException("Failed to upload file to Supabase", e);
        }
    }

    public InputStream downloadFile(String storagePath) {
        try {
            String downloadUrl = supabaseUrl + "/storage/v1/object/" + storagePath;
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + supabaseAnonKey);
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return connection.getInputStream();
            } else {
                String errorMessage = readErrorResponse(connection);
                logger.error("Failed to download file from Supabase. Response code: {}, Error: {}", responseCode, errorMessage);
                throw new RuntimeException("Failed to download file from Supabase: " + errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error downloading file from Supabase: {}", storagePath, e);
            throw new RuntimeException("Failed to download file from Supabase", e);
        }
    }

    public void deleteFile(String storagePath) {
        try {
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + storagePath;
            URL url = new URL(deleteUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Authorization", "Bearer " + supabaseServiceKey);
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Deleted file from Supabase: {}", storagePath);
            } else if (responseCode == 404) {
                logger.warn("File not found for deletion in Supabase: {}", storagePath);
            } else {
                String errorMessage = readErrorResponse(connection);
                logger.error("Failed to delete file from Supabase. Response code: {}, Error: {}", responseCode, errorMessage);
                throw new RuntimeException("Failed to delete file from Supabase: " + errorMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting file from Supabase: {}", storagePath, e);
            throw new RuntimeException("Failed to delete file from Supabase", e);
        }
    }

    public void deleteAllFilesInSpace(String spaceId) {
        try {
            // List all files in the space
            String listUrl = supabaseUrl + "/storage/v1/object/list/spaces/" + spaceId + "/files";
            URL url = new URL(listUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + supabaseServiceKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Send empty JSON body for listing
            try (OutputStream os = connection.getOutputStream()) {
                os.write("{}".getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                String response = readResponse(connection);
                JsonObject[] files = gson.fromJson(response, JsonObject[].class);
                
                // Delete each file
                for (JsonObject file : files) {
                    String fileName = file.get("name").getAsString();
                    String filePath = "spaces/" + spaceId + "/files/" + fileName;
                    deleteFile(filePath);
                }
                
                logger.info("Deleted all files in space: {}", spaceId);
            } else {
                String errorMessage = readErrorResponse(connection);
                logger.error("Failed to list files in space. Response code: {}, Error: {}", responseCode, errorMessage);
                throw new RuntimeException("Failed to list files in space: " + errorMessage);
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

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String readErrorResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }
}
