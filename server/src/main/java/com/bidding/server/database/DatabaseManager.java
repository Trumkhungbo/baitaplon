package com.bidding.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton Pattern: Đảm bảo chỉ có 1 Connection duy nhất tới SQLite.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.dir") + "/data/auction.db";// file SQLite nằm trong <user.dir>/data

    private static DatabaseManager instance;// instance duy nhất của DatabaseManager
    private Connection connection;// kết nối tới database

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            new java.io.File("data").mkdirs(); // tạo thư mục data/ nếu chưa có
            connection = DriverManager.getConnection(DB_URL);// kết nối tới SQLite
            connection.setAutoCommit(true);// tự động commit sau mỗi câu lệnh SQL
            System.out.println("[DB] Kết nối SQLite thành công: " + DB_URL);//
        } catch (ClassNotFoundException | SQLException e) {//
            throw new RuntimeException("Không thể kết nối database: " + e.getMessage(), e);//
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
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Mất kết nối DB", e);
        }
        return connection;
    }
}