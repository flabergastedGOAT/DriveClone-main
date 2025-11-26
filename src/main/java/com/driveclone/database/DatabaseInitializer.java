package com.driveclone.database;

import com.driveclone.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final Config config;

    public DatabaseInitializer() {
        this.config = Config.getInstance();
    }

    public void initializeDatabase() {
        String dbPath = config.getDbPath();
        logger.info("Initializing database at: {}", dbPath);

        try {
            // Check if database file exists
            Path dbFile = Paths.get(dbPath);
            boolean dbExists = Files.exists(dbFile);

            // Create database connection
            String url = "jdbc:sqlite:" + dbPath;
            try (Connection connection = DriverManager.getConnection(url)) {
                logger.info("Connected to SQLite database: {}", dbPath);

                if (!dbExists) {
                    logger.info("Database file does not exist, creating schema...");
                    createSchema(connection);
                    logger.info("Database schema created successfully");
                } else {
                    logger.info("Database file exists, skipping schema creation");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        try (InputStream schemaStream = getClass().getResourceAsStream("/schema.sql");
             BufferedReader reader = new BufferedReader(new InputStreamReader(schemaStream))) {

            StringBuilder schema = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                schema.append(line).append("\n");
            }

            // Split schema into individual statements
            String[] statements = schema.toString().split(";");
            
            try (Statement statement = connection.createStatement()) {
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty() && !sql.startsWith("--")) {
                        logger.debug("Executing SQL: {}", sql);
                        statement.execute(sql);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read schema file", e);
            throw new RuntimeException("Failed to read schema file", e);
        }
    }

    public static void main(String[] args) {
        // Allow running this class directly for database initialization
        DatabaseInitializer initializer = new DatabaseInitializer();
        initializer.initializeDatabase();
        System.out.println("Database initialization completed successfully!");
    }
}
