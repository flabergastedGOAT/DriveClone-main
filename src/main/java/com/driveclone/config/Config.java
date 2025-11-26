package com.driveclone.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static Config instance;
    private final Map<String, String> envVars;

    private Config() {
        this.envVars = new HashMap<>();
        loadEnvironmentVariables();
        logger.info("Configuration loaded successfully");
    }

    private void loadEnvironmentVariables() {
        // Load from .env file if it exists
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        envVars.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Could not load .env file, using system environment variables only");
        }
    }

    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    // Server Configuration
    public int getPort() {
        String portStr = getEnvVar("PORT", "8080");
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid PORT value '{}', using default 8080", portStr);
            return 8080;
        }
    }

    // Database Configuration
    public String getDbPath() {
        return getEnvVar("DB_PATH", "driveclone.db");
    }

    // Firebase Configuration
    public String getFirebaseProjectId() {
        return getEnvVar("FIREBASE_PROJECT_ID");
    }

    public String getFirebaseWebClientId() {
        return getEnvVar("FIREBASE_WEB_CLIENT_ID");
    }

    public String getGoogleApplicationCredentials() {
        return getEnvVar("GOOGLE_APPLICATION_CREDENTIALS");
    }

    public String getFirebaseServiceAccount() {
        return getEnvVar("FIREBASE_SERVICE_ACCOUNT");
    }

    // Supabase Configuration
    public String getSupabaseUrl() {
        return getEnvVar("SUPABASE_URL");
    }

    public String getSupabaseAnonKey() {
        return getEnvVar("SUPABASE_ANON_KEY");
    }

    public String getSupabaseServiceRoleKey() {
        return getEnvVar("SUPABASE_SERVICE_ROLE_KEY");
    }

    // Storage Configuration
    public String getStorageMode() {
        return getEnvVar("STORAGE_MODE", "supabase");
    }

    // Admin Configuration
    public String getAdminEmail() {
        return getEnvVar("ADMIN_EMAIL");
    }

    public String getJwtSecret() {
        return getEnvVar("JWT_SECRET", "default-jwt-secret-for-development");
    }

    private String getEnvVar(String key) {
        return getEnvVar(key, null);
    }

    private String getEnvVar(String key, String defaultValue) {
        // First check .env file, then system environment variables
        String value = envVars.get(key);
        if (value == null) {
            value = System.getenv(key);
        }
        
        if (value == null || value.trim().isEmpty()) {
            if (defaultValue != null) {
                logger.warn("Environment variable {} not set, using default value: {}", key, defaultValue);
                return defaultValue;
            } else {
                logger.warn("Environment variable {} is not set - this may cause issues", key);
                return null;
            }
        }
        return value;
    }
}
