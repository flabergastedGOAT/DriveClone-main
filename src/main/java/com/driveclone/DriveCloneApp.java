package com.driveclone;

import com.driveclone.model.Space;
import com.driveclone.model.SpaceFile;
import com.driveclone.service.SpaceService;
import com.driveclone.util.GsonConfig;
import com.driveclone.util.JsonResponse;
import com.driveclone.auth.NextAuthJwtVerifier;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.MultipartConfigElement;

public class DriveCloneApp {
    private static final Logger logger = LoggerFactory.getLogger(DriveCloneApp.class);
    private static final Gson gson = GsonConfig.getGson();

    private SpaceService spaceService;

    public DriveCloneApp() {
        // Initialize services
        this.spaceService = new SpaceService();
    }

    public static void main(String[] args) {
        new DriveCloneApp().start();
    }


    public void start() {
        try {
            // Database will be initialized by SqliteMetadataService when needed
            System.out.println("✅ SQLite ready");
            logger.info("✅ SQLite ready");
            
            // Initialize Gson with LocalDateTime adapter
            System.out.println("✅ Gson LocalDateTime adapter registered");
            logger.info("✅ Gson LocalDateTime adapter registered");
            
            // Initialize services
            System.out.println("✅ Services initialized successfully");
            logger.info("✅ Services initialized successfully");
            
            // Set port from configuration
            int port = com.driveclone.config.Config.getInstance().getPort();
            Spark.port(port);
            logger.info("Starting DriveClone server on port: {}", port);

            // Configure static files
            Spark.staticFiles.location("/public");
            
            // Configure multipart for file uploads
            Spark.before("/api/spaces/*/files", (request, response) -> {
                if ("POST".equals(request.requestMethod())) {
                    request.raw().setAttribute("org.eclipse.jetty.multipartConfig", 
                        new MultipartConfigElement("/tmp", 100000000, 100000000, 100000000));
                }
            });

            // Enable CORS for API routes only
            Spark.before("/api/*", (request, response) -> {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            });

            // Handle preflight requests for API
            Spark.options("/api/*", (request, response) -> {
                response.status(200);
                return "";
            });

            // NextAuth JWT token verification middleware for protected API routes
            Spark.before("/api/*", (request, response) -> {
                // Skip authentication for login endpoint (if needed)
                if (request.uri().equals("/api/login")) {
                    return;
                }
                
                System.out.println("🔒 Authentication middleware triggered for: " + request.uri());
                
                String authHeader = request.headers("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    System.out.println("❌ No Authorization header found");
                    response.status(401);
                    response.type("application/json");
                    response.body(gson.toJson(Map.of("error", "Authentication failed")));
                    Spark.halt();
                    return;
                }
                
                String token = authHeader.substring(7);
                try {
                    System.out.println("🔍 Verifying NextAuth token...");
                    com.driveclone.model.User user = NextAuthJwtVerifier.verifyToken(token);
                    
                    // Store user in request attributes for use in route handlers
                    request.attribute("user", user);
                    System.out.println("✅ Token verified for user: " + user.getEmail());
                } catch (Exception e) {
                    System.out.println("❌ Token verification failed: " + e.getMessage());
                    logger.warn("Token verification failed: {}", e.getMessage());
                    response.status(401);
                    response.type("application/json");
                    response.body(gson.toJson(Map.of("error", "Authentication failed: " + e.getMessage())));
                    Spark.halt();
                }
            });

            // Routes
            setupRoutes();

            // Print success message
            System.out.println("✅ DriveClone running at http://localhost:" + port);
            logger.info("DriveClone server started successfully on port: {}", port);
        } catch (Exception e) {
            logger.error("Failed to start DriveClone server", e);
            throw new RuntimeException("Failed to start server", e);
        }
    }

    private void setupRoutes() {
        // Serve index.html at root
        Spark.get("/", (request, response) -> {
            response.type("text/html");
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/public/index.html");
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    return new String(bytes, "UTF-8");
                } else {
                    response.status(404);
                    return "<html><body><h1>HTML file not found</h1></body></html>";
                }
            } catch (Exception e) {
                response.status(500);
                return "<html><body><h1>Error loading HTML file: " + e.getMessage() + "</h1></body></html>";
            }
        });
        
        // Static files are served automatically by Spark.staticFiles.location("/public")

        // Railway port verification route
        Spark.get("/status", (request, response) -> {
            response.type("text/plain");
            return "🚀 DriveClone is running on port " + Spark.port();
        });


        // Health check
        Spark.get("/ping", this::ping);
        
        // Test endpoint for LocalDateTime serialization
        Spark.get("/api/test-datetime", this::testDateTime);

        // API Routes (all under /api/)
        // Authentication routes
        Spark.post("/api/login", this::login);

        // Space routes
        Spark.get("/api/spaces", this::getSpaces);
        Spark.post("/api/spaces", this::createSpace);
        Spark.get("/api/spaces/:id", this::getSpace);
        Spark.put("/api/spaces/:id", this::updateSpace);
        Spark.delete("/api/spaces/:id", this::deleteSpace);
        Spark.post("/api/spaces/:id/members", this::addMemberToSpace);
        Spark.put("/api/spaces/:id/members/:email", this::updateMemberRole);
        Spark.delete("/api/spaces/:id/members/:email", this::removeMemberFromSpace);

        // File routes
        Spark.get("/api/spaces/:spaceId/files", this::getFiles);
        Spark.post("/api/spaces/:spaceId/files", this::uploadFile);
        Spark.get("/api/files/:fileId", this::downloadFile);
        Spark.delete("/api/files/:fileId", this::deleteFile);

        // Activity routes
        Spark.get("/api/spaces/:spaceId/activity", this::getActivityLog);

        // Global exception handler for all unhandled exceptions
        Spark.exception(Exception.class, (exception, request, response) -> {
            logger.error("Unhandled exception in " + request.requestMethod() + " " + request.uri(), exception);
            System.out.println("❌ Unhandled exception: " + exception.getMessage());
            exception.printStackTrace();
            
            String errorMessage = "Internal server error";
            if (exception.getMessage() != null) {
                errorMessage += ": " + exception.getMessage();
            }
            
            response.body(JsonResponse.internalError(response, errorMessage));
        });
    }

    private Object ping(Request request, Response response) {
        response.type("text/plain");
        return "OK";
    }
    
    private Object testDateTime(Request request, Response response) {
        response.type("application/json");
        
        // Create a test Space object with LocalDateTime
        Space testSpace = new Space("Test Space", "Test Description", "admin123", "admin@test.com");
        testSpace.setId("test-space-123");
        
        // This should serialize without errors thanks to the LocalDateTime adapter
        return gson.toJson(testSpace);
    }

    private Object login(Request request, Response response) {
        try {
            JsonObject body = gson.fromJson(request.body(), JsonObject.class);
            String token = body.get("token").getAsString();

            // Verify NextAuth token
            com.driveclone.model.User user = NextAuthJwtVerifier.verifyToken(token);

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, Map.of(
                "success", true,
                "user", user,
                "message", "Authentication successful"
            ));
        } catch (Exception e) {
            logger.error("Login error", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 401, null);
            return JsonResponse.unauthorized(response, "Authentication failed: " + e.getMessage());
        }
    }

    private Object getSpaces(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            
            if (spaceService == null) {
                // Return empty list when service is not available
                System.out.println("📝 SpaceService not available, returning empty list");
                System.out.println("✅ JSON response built for /api/spaces");
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
                return JsonResponse.success(response, Collections.emptyList());
            }
            
            List<Space> spaces = spaceService.getSpacesForUser(user.getEmail());

            // Ensure we always return a valid JSON array, never null
            List<Space> safeSpaces = JsonResponse.ensureList(spaces);
            
            System.out.println("✅ JSON response built for /api/spaces");
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, safeSpaces);
        } catch (Exception e) {
            logger.error("Error getting spaces", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to get spaces: " + e.getMessage());
        }
    }

    private Object createSpace(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            System.out.println("🔍 Creating space for user: " + user.getEmail() + " (UID: " + user.getFirebaseUid() + ")");
            
            JsonObject body = gson.fromJson(request.body(), JsonObject.class);

            String name = body.get("name").getAsString();
            String description = body.has("description") ? body.get("description").getAsString() : "";
            
            System.out.println("🔍 Space details - Name: " + name + ", Description: " + description);

            String spaceId = spaceService.createSpace(name, description, user.getFirebaseUid(), user.getEmail());
            System.out.println("✅ Space created with ID: " + spaceId);
            
            Optional<Space> space = spaceService.getSpace(spaceId);

            if (space.isPresent()) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 201, user.getEmail());
                return JsonResponse.success(response, space.get(), 201);
            } else {
                System.out.println("❌ Failed to retrieve created space with ID: " + spaceId);
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, user.getEmail());
                return JsonResponse.internalError(response, "Failed to retrieve created space");
            }
        } catch (Exception e) {
            logger.error("Error creating space", e);
            System.out.println("❌ Error creating space: " + e.getMessage());
            e.printStackTrace();
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to create space: " + e.getMessage());
        }
    }

    private Object getSpace(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":id");

            Optional<Space> space = spaceService.getSpace(spaceId);
            if (space.isEmpty()) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 404, user.getEmail());
                return JsonResponse.notFound(response, "Space not found");
            }

            if (!spaceService.isUserMemberOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Access denied");
            }

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, space.get());
        } catch (Exception e) {
            logger.error("Error getting space", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to get space: " + e.getMessage());
        }
    }

    private Object updateSpace(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":id");
            JsonObject body = gson.fromJson(request.body(), JsonObject.class);

            if (!spaceService.isUserAdminOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Only admins can update spaces");
            }

            Optional<Space> spaceOpt = spaceService.getSpace(spaceId);
            if (spaceOpt.isEmpty()) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 404, user.getEmail());
                return JsonResponse.notFound(response, "Space not found");
            }

            Space space = spaceOpt.get();
            if (body.has("name")) {
                space.setName(body.get("name").getAsString());
            }
            if (body.has("description")) {
                space.setDescription(body.get("description").getAsString());
            }

            spaceService.updateSpace(space);

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, space);
        } catch (Exception e) {
            logger.error("Error updating space", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to update space: " + e.getMessage());
        }
    }

    private Object deleteSpace(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":id");

            if (!spaceService.isUserAdminOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Only admins can delete spaces");
            }

            spaceService.deleteSpace(spaceId);

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, Map.of("message", "Space deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting space", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to delete space: " + e.getMessage());
        }
    }

    private Object addMemberToSpace(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":id");
            JsonObject body = gson.fromJson(request.body(), JsonObject.class);

            if (!spaceService.isUserAdminOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Only admins can add members");
            }

            String memberEmail = body.get("email").getAsString();
            Optional<Space> spaceOpt = spaceService.getSpace(spaceId);
            if (spaceOpt.isEmpty()) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 404, user.getEmail());
                return JsonResponse.notFound(response, "Space not found");
            }

            if (spaceOpt.get().getAdminEmail().equalsIgnoreCase(memberEmail)) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 400, user.getEmail());
                return JsonResponse.badRequest(response, "User is already the owner");
            }

            spaceService.addMemberToSpace(spaceId, memberEmail, user.getEmail());

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, Map.of("message", "Member added successfully"));
        } catch (Exception e) {
            logger.error("Error adding member to space", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to add member: " + e.getMessage());
        }
    }

    private Object removeMemberFromSpace(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":id");
            String memberEmail = request.params(":email");

            if (!spaceService.isUserAdminOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Only admins can remove members");
            }

            Optional<Space> spaceOpt = spaceService.getSpace(spaceId);
            if (spaceOpt.isEmpty()) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 404, user.getEmail());
                return JsonResponse.notFound(response, "Space not found");
            }

            Space space = spaceOpt.get();
            if (space.getAdminEmail().equalsIgnoreCase(memberEmail)) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 400, user.getEmail());
                return JsonResponse.badRequest(response, "Cannot remove the space owner");
            }

            spaceService.removeMemberFromSpace(spaceId, memberEmail, user.getEmail());

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, Map.of("message", "Member removed successfully"));
        } catch (Exception e) {
            logger.error("Error removing member from space", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to remove member: " + e.getMessage());
        }
    }

    private Object updateMemberRole(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":id");
            String memberEmail = request.params(":email");

            if (!spaceService.isUserAdminOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Only admins can update member roles");
            }

            Optional<Space> spaceOpt = spaceService.getSpace(spaceId);
            if (spaceOpt.isEmpty()) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 404, user.getEmail());
                return JsonResponse.notFound(response, "Space not found");
            }

            Space space = spaceOpt.get();
            if (space.getAdminEmail().equalsIgnoreCase(memberEmail)) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 400, user.getEmail());
                return JsonResponse.badRequest(response, "Cannot change the owner's role");
            }

            JsonObject body = gson.fromJson(request.body(), JsonObject.class);
            String role = body.has("role") ? body.get("role").getAsString() : "MEMBER";

            spaceService.updateMemberRole(spaceId, memberEmail, role, user.getEmail());

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, Map.of("message", "Member role updated"));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid member role", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 400, null);
            return JsonResponse.badRequest(response, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating member role", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to update member role: " + e.getMessage());
        }
    }

    private Object getFiles(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":spaceId");

            if (!spaceService.isUserMemberOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Access denied");
            }

            List<SpaceFile> files = spaceService.getFilesForSpace(spaceId);
            
            // Ensure we always return a valid JSON array, never null
            List<SpaceFile> safeFiles = JsonResponse.ensureList(files);
            
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, safeFiles);
        } catch (Exception e) {
            logger.error("Error getting files", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to get files: " + e.getMessage());
        }
    }

    private Object uploadFile(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":spaceId");
            
            System.out.println("🔍 Uploading file to space: " + spaceId);
            System.out.println("🔍 Content-Type: " + request.contentType());

            if (!spaceService.isUserMemberOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Access denied");
            }

            // Ensure multipart/form-data
            if (!request.contentType().startsWith("multipart/form-data")) {
                System.out.println("❌ Invalid content type: " + request.contentType());
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 400, user.getEmail());
                return JsonResponse.badRequest(response, "Content-Type must be multipart/form-data");
            }

            // Get file from multipart form data
            javax.servlet.http.Part uploadedFile = request.raw().getPart("file");
            if (uploadedFile == null) {
                System.out.println("❌ No file part found in request");
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 400, user.getEmail());
                return JsonResponse.badRequest(response, "No file provided");
            }

            String originalFilename = uploadedFile.getSubmittedFileName();
            String contentType = uploadedFile.getContentType();
            long size = uploadedFile.getSize();
            InputStream inputStream = uploadedFile.getInputStream();
            
            System.out.println("✅ File details - Name: " + originalFilename + ", Size: " + size + ", Type: " + contentType);

            String fileId = spaceService.uploadFile(inputStream, spaceId, originalFilename, 
                                                  contentType, size, user.getFirebaseUid(), user.getEmail());
            
            System.out.println("✅ File uploaded successfully with ID: " + fileId);

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 201, user.getEmail());
            return JsonResponse.success(response, Map.of("fileId", fileId, "message", "File uploaded successfully"), 201);
        } catch (Exception e) {
            logger.error("Error uploading file", e);
            System.out.println("❌ Upload error: " + e.getMessage());
            e.printStackTrace();
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to upload file: " + e.getMessage());
        }
    }

    private Object downloadFile(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String fileId = request.params(":fileId");

            Optional<SpaceFile> fileOpt = spaceService.getFile(fileId);
            if (fileOpt.isEmpty()) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 404, user.getEmail());
                return JsonResponse.notFound(response, "File not found");
            }

            SpaceFile file = fileOpt.get();
            InputStream inputStream = spaceService.downloadFile(fileId, user.getEmail());

            response.type(file.getContentType());
            response.header("Content-Disposition", "attachment; filename=\"" + file.getOriginalFilename() + "\"");
            
            // Stream the file content
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                response.raw().getOutputStream().write(buffer, 0, bytesRead);
            }
            inputStream.close();
            
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return "";
        } catch (Exception e) {
            logger.error("Error downloading file", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to download file: " + e.getMessage());
        }
    }

    private Object deleteFile(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String fileId = request.params(":fileId");

            spaceService.deleteFile(fileId, user.getEmail());

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting file", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to delete file: " + e.getMessage());
        }
    }

    private Object getActivityLog(Request request, Response response) {
        try {
            com.driveclone.model.User user = request.attribute("user");
            String spaceId = request.params(":spaceId");

            if (!spaceService.isUserMemberOfSpace(spaceId, user.getEmail())) {
                JsonResponse.logApiCall(request.requestMethod(), request.uri(), 403, user.getEmail());
                return JsonResponse.forbidden(response, "Access denied");
            }

            List<com.driveclone.model.Activity> activities = spaceService.getActivityLog(spaceId);

            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 200, user.getEmail());
            return JsonResponse.success(response, activities);
        } catch (Exception e) {
            logger.error("Error getting activity log", e);
            JsonResponse.logApiCall(request.requestMethod(), request.uri(), 500, null);
            return JsonResponse.internalError(response, "Failed to get activity log: " + e.getMessage());
        }
    }

}