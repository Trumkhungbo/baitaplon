
package com.bidding.server.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final Path DB_PATH = resolveDatabasePath();
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            Files.createDirectories(DB_PATH.getParent());

            // Bật WAL mode và cấu hình một lần khi khởi động
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");   // nhiều reader + 1 writer đồng thời
                st.execute("PRAGMA busy_timeout=5000");  // chờ tối đa 5s nếu DB bị lock
                st.execute("PRAGMA synchronous=NORMAL"); // cân bằng giữa an toàn và tốc độ
                st.execute("PRAGMA cache_size=-8000");   // cache 8MB trong RAM
                System.out.println("[DB] SQLite WAL mode đã bật: " + DB_URL);
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi tạo database: " + e.getMessage(), e);
        }
    }

    private static Path resolveDatabasePath() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path projectRoot = findProjectRoot(current);
        return projectRoot.resolve("data").resolve("auction.db").normalize();
    }

    private static Path findProjectRoot(Path start) {
        Path current = start;

        while (current != null) {
            if (Files.exists(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("server"))
                    && Files.isDirectory(current.resolve("client"))
                    && Files.isDirectory(current.resolve("common"))) {
                return current;
            }

            current = current.getParent();
        }

        return start;
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    
    public Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Không thể mở kết nối DB: " + e.getMessage(), e);
        }
    }
}
