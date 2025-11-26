package com.driveclone.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;

/**
 * Utility class for configuring Gson with custom type adapters.
 * This ensures proper serialization/deserialization of Java 8 time types.
 */
public class GsonConfig {
    
    private static Gson gson;
    
    /**
     * Get a configured Gson instance with LocalDateTime adapter.
     * @return Configured Gson instance
     */
    public static Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();
        }
        return gson;
    }
    
    /**
     * Create a new Gson instance with LocalDateTime adapter.
     * @return New configured Gson instance
     */
    public static Gson createGson() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();
    }
}
