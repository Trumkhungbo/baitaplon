
package com.bidding.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:data/auction.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            new java.io.File("data").mkdirs();

            // Bật WAL mode và cấu hình một lần khi khởi động
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");   // nhiều reader + 1 writer đồng thời
                st.execute("PRAGMA busy_timeout=5000");  // chờ tối đa 5s nếu DB bị lock
                st.execute("PRAGMA synchronous=NORMAL"); // cân bằng giữa an toàn và tốc độ
                st.execute("PRAGMA cache_size=-8000");   // cache 8MB trong RAM
                System.out.println("[DB] SQLite WAL mode đã bật: " + DB_URL);
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Không thể khởi tạo database: " + e.getMessage(), e);
        }
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