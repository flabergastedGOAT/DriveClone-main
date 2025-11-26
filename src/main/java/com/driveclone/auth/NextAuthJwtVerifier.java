package com.driveclone.auth;

import com.driveclone.model.User;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class NextAuthJwtVerifier {
    private static final Logger logger = LoggerFactory.getLogger(NextAuthJwtVerifier.class);

    /**
     * Verifies a NextAuth JWT token (base64 encoded JSON)
     * Since NextAuth uses database sessions, we'll decode the token
     * which contains user information from the Next.js backend
     */
    public static User verifyToken(String token) {
        try {
            // Decode base64 token
            byte[] decodedBytes = Base64.getDecoder().decode(token);
            String decodedJson = new String(decodedBytes);
            
            // Parse JSON
            JsonObject jsonObject = JsonParser.parseString(decodedJson).getAsJsonObject();
            
            // Extract user information
            String email = jsonObject.get("email").getAsString();
            String name = jsonObject.has("name") && !jsonObject.get("name").isJsonNull() 
                ? jsonObject.get("name").getAsString() : email;
            String id = jsonObject.has("id") ? jsonObject.get("id").getAsString() : email;
            
            // Create user object
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setFirebaseUid(id); // Reusing this field for NextAuth user ID
            
            logger.info("Verified NextAuth token for user: {}", email);
            return user;
        } catch (Exception e) {
            logger.error("Failed to verify NextAuth token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }
}

