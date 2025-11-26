package com.driveclone.service;

import com.driveclone.model.Activity;
import com.driveclone.model.Space;
import com.driveclone.model.SpaceFile;
import com.driveclone.model.SpaceMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SqliteMetadataService {
    private static final Logger logger = LoggerFactory.getLogger(SqliteMetadataService.class);
    private final String dbPath;

    public SqliteMetadataService() {
        this.dbPath = com.driveclone.config.Config.getInstance().getDbPath();
        initializeTables();
    }

    private void initializeTables() {
        try (Connection conn = getConnection()) {
            // Create spaces table
            String createSpacesTable = """
                CREATE TABLE IF NOT EXISTS spaces (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT,
                    admin_id TEXT NOT NULL,
                    admin_email TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    member_emails TEXT
                )
            """;
            
            // Create members table
            String createMembersTable = """
                CREATE TABLE IF NOT EXISTS space_members (
                    id TEXT PRIMARY KEY,
                    space_id TEXT NOT NULL,
                    member_email TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'MEMBER',
                    added_at TEXT NOT NULL,
                    FOREIGN KEY (space_id) REFERENCES spaces (id) ON DELETE CASCADE,
                    UNIQUE(space_id, member_email)
                )
            """;
            
            // Create files table
            String createFilesTable = """
                CREATE TABLE IF NOT EXISTS space_files (
                    id TEXT PRIMARY KEY,
                    space_id TEXT NOT NULL,
                    original_filename TEXT NOT NULL,
                    storage_path TEXT NOT NULL,
                    content_type TEXT,
                    size INTEGER NOT NULL,
                    uploader_id TEXT NOT NULL,
                    uploader_email TEXT NOT NULL,
                    uploaded_at TEXT NOT NULL,
                    FOREIGN KEY (space_id) REFERENCES spaces (id)
                )
            """;

            // Create activity table
            String createActivityTable = """
                CREATE TABLE IF NOT EXISTS activity (
                    id TEXT PRIMARY KEY,
                    space_id TEXT NOT NULL,
                    user_email TEXT NOT NULL,
                    action TEXT NOT NULL,
                    details TEXT,
                    timestamp TEXT NOT NULL,
                    FOREIGN KEY (space_id) REFERENCES spaces (id)
                )
            """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createSpacesTable);
                stmt.execute(createMembersTable);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_space_members_space_id ON space_members(space_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_space_members_email ON space_members(member_email)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_space_members_role ON space_members(role)");
                stmt.execute(createFilesTable);
                stmt.execute(createActivityTable);
                logger.info("SQLite metadata tables initialized successfully");
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize SQLite metadata tables", e);
            throw new RuntimeException("Failed to initialize SQLite metadata tables", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    // Space operations
    public String createSpace(Space space) {
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO spaces (id, name, description, admin_id, admin_email, created_at, member_emails) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, space.getId());
                stmt.setString(2, space.getName());
                stmt.setString(3, space.getDescription());
                stmt.setString(4, space.getAdminId());
                stmt.setString(5, space.getAdminEmail());
                stmt.setString(6, space.getCreatedAt().toString());
                
                // Store member emails as comma-separated string
                String memberEmails = space.getMemberEmails() != null ? 
                    String.join(",", space.getMemberEmails()) : "";
                stmt.setString(7, memberEmails);
                
                stmt.executeUpdate();
                logger.info("Created space: {}", space.getId());
                return space.getId();
            }
        } catch (SQLException e) {
            logger.error("Error creating space", e);
            throw new RuntimeException("Failed to create space", e);
        }
    }

    public Optional<Space> getSpace(String spaceId) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM spaces WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, spaceId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Space space = mapToSpace(rs);
                        populateMembers(conn, space);
                        return Optional.of(space);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting space: {}", spaceId, e);
        }
        return Optional.empty();
    }

    public List<Space> getSpacesForUser(String userEmail) {
        List<Space> spaces = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM spaces WHERE admin_email = ? OR member_emails LIKE ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userEmail);
                stmt.setString(2, "%" + userEmail + "%");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Space space = mapToSpace(rs);
                        populateMembers(conn, space);
                        spaces.add(space);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting spaces for user: {}", userEmail, e);
        }
        return spaces;
    }

    public void updateSpace(Space space) {
        try (Connection conn = getConnection()) {
            String sql = "UPDATE spaces SET name = ?, description = ?, member_emails = ? WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, space.getName());
                stmt.setString(2, space.getDescription());
                
                String memberEmails = space.getMemberEmails() != null ? 
                    String.join(",", space.getMemberEmails()) : "";
                stmt.setString(3, memberEmails);
                stmt.setString(4, space.getId());
                
                stmt.executeUpdate();
                logger.info("Updated space: {}", space.getId());
            }
        } catch (SQLException e) {
            logger.error("Error updating space: {}", space.getId(), e);
            throw new RuntimeException("Failed to update space", e);
        }
    }

    public void deleteSpace(String spaceId) {
        try (Connection conn = getConnection()) {
            // Delete files first (foreign key constraint)
            String deleteFilesSql = "DELETE FROM space_files WHERE space_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteFilesSql)) {
                stmt.setString(1, spaceId);
                stmt.executeUpdate();
            }

            // Delete members
            String deleteMembersSql = "DELETE FROM space_members WHERE space_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteMembersSql)) {
                stmt.setString(1, spaceId);
                stmt.executeUpdate();
            }
            
            // Delete space
            String deleteSpaceSql = "DELETE FROM spaces WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSpaceSql)) {
                stmt.setString(1, spaceId);
                stmt.executeUpdate();
            }
            
            logger.info("Deleted space: {}", spaceId);
        } catch (SQLException e) {
            logger.error("Error deleting space: {}", spaceId, e);
            throw new RuntimeException("Failed to delete space", e);
        }
    }

    public void addMemberToSpace(String spaceId, String memberEmail) {
        try (Connection conn = getConnection()) {
            String insertSql = "INSERT OR IGNORE INTO space_members (id, space_id, member_email, role, added_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, spaceId);
                stmt.setString(3, memberEmail);
                stmt.setString(4, "MEMBER");
                stmt.setString(5, LocalDateTime.now().toString());
                stmt.executeUpdate();
            }

            syncMemberEmails(conn, spaceId);
            logger.info("Added member {} to space {}", memberEmail, spaceId);
        } catch (SQLException e) {
            logger.error("Error adding member to space", e);
            throw new RuntimeException("Failed to add member to space", e);
        }
    }

    public void removeMemberFromSpace(String spaceId, String memberEmail) {
        try (Connection conn = getConnection()) {
            String deleteSql = "DELETE FROM space_members WHERE space_id = ? AND member_email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setString(1, spaceId);
                stmt.setString(2, memberEmail);
                stmt.executeUpdate();
            }

            syncMemberEmails(conn, spaceId);
            logger.info("Removed member {} from space {}", memberEmail, spaceId);
        } catch (SQLException e) {
            logger.error("Error removing member from space", e);
            throw new RuntimeException("Failed to remove member from space", e);
        }
    }

    public void updateMemberRole(String spaceId, String memberEmail, String role) {
        try (Connection conn = getConnection()) {
            String normalizedRole = role == null ? "MEMBER" : role.toUpperCase();
            if (!normalizedRole.equals("ADMIN") && !normalizedRole.equals("MEMBER")) {
                throw new IllegalArgumentException("Invalid member role: " + role);
            }

            String updateSql = "UPDATE space_members SET role = ? WHERE space_id = ? AND member_email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, normalizedRole);
                stmt.setString(2, spaceId);
                stmt.setString(3, memberEmail);
                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    throw new RuntimeException("Member not found in space");
                }
            }

            logger.info("Updated member {} role to {} in space {}", memberEmail, normalizedRole, spaceId);
        } catch (SQLException e) {
            logger.error("Error updating member role", e);
            throw new RuntimeException("Failed to update member role", e);
        }
    }

    public boolean isUserMemberOfSpace(String spaceId, String userEmail) {
        try (Connection conn = getConnection()) {
            if (isSpaceOwner(conn, spaceId, userEmail)) {
                return true;
            }

            String sql = "SELECT 1 FROM space_members WHERE space_id = ? AND member_email = ? LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, spaceId);
                stmt.setString(2, userEmail);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking membership", e);
            return false;
        }
    }

    public boolean isUserAdminOfSpace(String spaceId, String userEmail) {
        try (Connection conn = getConnection()) {
            if (isSpaceOwner(conn, spaceId, userEmail)) {
                return true;
            }

            String sql = "SELECT role FROM space_members WHERE space_id = ? AND member_email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, spaceId);
                stmt.setString(2, userEmail);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return "ADMIN".equalsIgnoreCase(rs.getString("role"));
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            logger.error("Error checking admin membership", e);
            return false;
        }
    }

    // File operations
    public String createFile(SpaceFile file) {
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO space_files (id, space_id, original_filename, storage_path, content_type, size, uploader_id, uploader_email, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, file.getId());
                stmt.setString(2, file.getSpaceId());
                stmt.setString(3, file.getOriginalFilename());
                stmt.setString(4, file.getStoragePath());
                stmt.setString(5, file.getContentType());
                stmt.setLong(6, file.getSize());
                stmt.setString(7, file.getUploaderId());
                stmt.setString(8, file.getUploaderEmail());
                stmt.setString(9, file.getUploadedAt().toString());
                
                stmt.executeUpdate();
                logger.info("Created file: {}", file.getId());
                return file.getId();
            }
        } catch (SQLException e) {
            logger.error("Error creating file", e);
            throw new RuntimeException("Failed to create file", e);
        }
    }

    public List<SpaceFile> getFilesForSpace(String spaceId) {
        List<SpaceFile> files = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM space_files WHERE space_id = ? ORDER BY uploaded_at DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, spaceId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        files.add(mapToSpaceFile(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting files for space: {}", spaceId, e);
        }
        return files;
    }

    public Optional<SpaceFile> getFile(String fileId) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM space_files WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapToSpaceFile(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting file: {}", fileId, e);
        }
        return Optional.empty();
    }

    public void deleteFile(String fileId) {
        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM space_files WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileId);
                stmt.executeUpdate();
            }
            
            logger.info("Deleted file: {}", fileId);
        } catch (SQLException e) {
            logger.error("Error deleting file: {}", fileId, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    // Helper methods
    private boolean isSpaceOwner(Connection conn, String spaceId, String userEmail) throws SQLException {
        String sql = "SELECT 1 FROM spaces WHERE id = ? AND admin_email = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, spaceId);
            stmt.setString(2, userEmail);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void syncMemberEmails(Connection conn, String spaceId) throws SQLException {
        String fetchSql = "SELECT member_email FROM space_members WHERE space_id = ? ORDER BY member_email";
        List<String> members = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(fetchSql)) {
            stmt.setString(1, spaceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("member_email"));
                }
            }
        }

        String updateSql = "UPDATE spaces SET member_emails = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, String.join(",", members));
            stmt.setString(2, spaceId);
            stmt.executeUpdate();
        }
    }

    private void populateMembers(Connection conn, Space space) throws SQLException {
        space.setMembers(fetchMembers(conn, space));
    }

    private List<SpaceMember> fetchMembers(Connection conn, Space space) throws SQLException {
        List<SpaceMember> members = new ArrayList<>();

        SpaceMember owner = new SpaceMember(space.getAdminEmail(), "ADMIN", space.getCreatedAt(), true);
        members.add(owner);

        String sql = "SELECT member_email, role, added_at FROM space_members WHERE space_id = ? ORDER BY datetime(added_at) ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, space.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SpaceMember member = new SpaceMember();
                    member.setEmail(rs.getString("member_email"));
                    member.setRole(Optional.ofNullable(rs.getString("role")).orElse("MEMBER"));
                    member.setAddedAt(parseTimestamp(rs.getString("added_at")));
                    member.setOwner(false);
                    members.add(member);
                }
            }
        }

        return members;
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isEmpty()) {
            return LocalDateTime.now();
        }
        return LocalDateTime.parse(value);
    }

    private Space mapToSpace(ResultSet rs) throws SQLException {
        Space space = new Space();
        space.setId(rs.getString("id"));
        space.setName(rs.getString("name"));
        space.setDescription(rs.getString("description"));
        space.setAdminId(rs.getString("admin_id"));
        space.setAdminEmail(rs.getString("admin_email"));
        space.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        
        String memberEmailsStr = rs.getString("member_emails");
        if (memberEmailsStr != null && !memberEmailsStr.isEmpty()) {
            space.setMemberEmails(Arrays.asList(memberEmailsStr.split(",")));
        } else {
            space.setMemberEmails(new ArrayList<>());
        }
        
        return space;
    }

    private SpaceFile mapToSpaceFile(ResultSet rs) throws SQLException {
        SpaceFile file = new SpaceFile();
        file.setId(rs.getString("id"));
        file.setSpaceId(rs.getString("space_id"));
        file.setOriginalFilename(rs.getString("original_filename"));
        file.setStoragePath(rs.getString("storage_path"));
        file.setContentType(rs.getString("content_type"));
        file.setSize(rs.getLong("size"));
        file.setUploaderId(rs.getString("uploader_id"));
        file.setUploaderEmail(rs.getString("uploader_email"));
        file.setUploadedAt(LocalDateTime.parse(rs.getString("uploaded_at")));
        
        return file;
    }

    // Activity logging methods
    public void logActivity(String spaceId, String userEmail, String action, String details) {
        String sql = "INSERT INTO activity (id, space_id, user_email, action, details, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            Activity activity = new Activity(spaceId, userEmail, action, details);
            activity.setId(UUID.randomUUID().toString());
            
            pstmt.setString(1, activity.getId());
            pstmt.setString(2, activity.getSpaceId());
            pstmt.setString(3, activity.getUserEmail());
            pstmt.setString(4, activity.getAction());
            pstmt.setString(5, activity.getDetails());
            pstmt.setString(6, activity.getTimestamp().toString());
            
            pstmt.executeUpdate();
            logger.info("Activity logged: {} by {} in space {}", action, userEmail, spaceId);
        } catch (SQLException e) {
            logger.error("Error logging activity", e);
            throw new RuntimeException("Failed to log activity", e);
        }
    }

    public List<Activity> getActivityLog(String spaceId) {
        List<Activity> activities = new ArrayList<>();
        String sql = "SELECT * FROM activity WHERE space_id = ? ORDER BY timestamp DESC LIMIT 50";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, spaceId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                activities.add(mapToActivity(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting activity log for space: {}", spaceId, e);
        }
        
        return activities;
    }

    private Activity mapToActivity(ResultSet rs) throws SQLException {
        Activity activity = new Activity();
        activity.setId(rs.getString("id"));
        activity.setSpaceId(rs.getString("space_id"));
        activity.setUserEmail(rs.getString("user_email"));
        activity.setAction(rs.getString("action"));
        activity.setDetails(rs.getString("details"));
        activity.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
        return activity;
    }
}
