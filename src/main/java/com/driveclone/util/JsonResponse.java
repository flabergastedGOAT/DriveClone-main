package com.driveclone.util;

import com.google.gson.Gson;
import spark.Response;

import java.util.List;
import java.util.Map;

/**
 * Utility class for consistent JSON responses across all API endpoints.
 * Ensures all responses are valid JSON and properly formatted.
 */
public class JsonResponse {
    
    private static final Gson gson = GsonConfig.getGson();
    
    /**
     * Send a successful JSON response with data.
     * @param response Spark Response object
     * @param data The data to serialize to JSON
     * @return JSON string
     */
    public static String success(Response response, Object data) {
        response.type("application/json");
        response.status(200);
        return gson.toJson(data);
    }
    
    /**
     * Send a successful JSON response with data and custom status code.
     * @param response Spark Response object
     * @param data The data to serialize to JSON
     * @param statusCode HTTP status code
     * @return JSON string
     */
    public static String success(Response response, Object data, int statusCode) {
        response.type("application/json");
        response.status(statusCode);
        return gson.toJson(data);
    }
    
    /**
     * Send an error response with message.
     * @param response Spark Response object
     * @param message Error message
     * @param statusCode HTTP status code
     * @return JSON string
     */
    public static String error(Response response, String message, int statusCode) {
        response.type("application/json");
        response.status(statusCode);
        return gson.toJson(Map.of("error", message));
    }
    
    /**
     * Send a 500 internal server error response.
     * @param response Spark Response object
     * @param message Error message
     * @return JSON string
     */
    public static String internalError(Response response, String message) {
        return error(response, message, 500);
    }
    
    /**
     * Send a 401 unauthorized error response.
     * @param response Spark Response object
     * @param message Error message
     * @return JSON string
     */
    public static String unauthorized(Response response, String message) {
        return error(response, message, 401);
    }
    
    /**
     * Send a 403 forbidden error response.
     * @param response Spark Response object
     * @param message Error message
     * @return JSON string
     */
    public static String forbidden(Response response, String message) {
        return error(response, message, 403);
    }
    
    /**
     * Send a 404 not found error response.
     * @param response Spark Response object
     * @param message Error message
     * @return JSON string
     */
    public static String notFound(Response response, String message) {
        return error(response, message, 404);
    }
    
    /**
     * Send a 400 bad request error response.
     * @param response Spark Response object
     * @param message Error message
     * @return JSON string
     */
    public static String badRequest(Response response, String message) {
        return error(response, message, 400);
    }
    
    /**
     * Send a 503 service unavailable error response.
     * @param response Spark Response object
     * @param message Error message
     * @return JSON string
     */
    public static String serviceUnavailable(Response response, String message) {
        return error(response, message, 503);
    }
    
    /**
     * Ensure a list is never null - returns empty list if null.
     * @param list The list to check
     * @return The list or empty list if null
     */
    public static <T> List<T> ensureList(List<T> list) {
        return list != null ? list : List.of();
    }
    
    /**
     * Log API call for debugging.
     * @param method HTTP method
     * @param uri Request URI
     * @param statusCode Response status code
     * @param userEmail User email (can be null)
     */
    public static void logApiCall(String method, String uri, int statusCode, String userEmail) {
        String userInfo = userEmail != null ? " (user: " + userEmail + ")" : "";
        System.out.println("📡 " + method + " " + uri + " -> " + statusCode + userInfo);
    }
}
