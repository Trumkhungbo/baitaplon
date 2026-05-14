
package com.bidding.server.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.bidding.server.core.PasswordHasher;

public class DatabaseInitializer {

    public static void initialize() {
        // Dùng try-with-resources: connection tự đóng sau khi xong
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {

            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        username      TEXT    NOT NULL UNIQUE,
                        password_hash TEXT    NOT NULL,
                        email         TEXT    NOT NULL UNIQUE,
                        phone         TEXT,
                        personal_id   TEXT,
                        role          TEXT    NOT NULL,
                        balance       REAL    DEFAULT 0,
                        rating        REAL    DEFAULT 5.0,
                        created_at    INTEGER NOT NULL
                    )
                    """);
            ensureColumnExists("users", "phone", "TEXT");
            ensureColumnExists("users", "personal_id", "TEXT");
            ensureColumnExists("users", "role", "TEXT NOT NULL DEFAULT 'BIDDER'");
            ensureColumnExists("users", "balance", "REAL DEFAULT 0");
            ensureColumnExists("users", "rating", "REAL DEFAULT 5.0");
            ensureColumnExists("users", "created_at", "INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)");

            st.execute("""
                    CREATE TABLE IF NOT EXISTS items (
                        id              INTEGER PRIMARY KEY AUTOINCREMENT,
                        name            TEXT    NOT NULL,
                        description     TEXT,
                        starting_price  REAL    NOT NULL,
                        item_type       TEXT    NOT NULL,
                        seller_username TEXT    NOT NULL,
                        artist          TEXT,
                        creation_year   INTEGER,
                        brand           TEXT,
                        warranty_months INTEGER,
                        engine_type     TEXT,
                        mileage         INTEGER,
                        image_url       TEXT
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS auctions (
                        id                      INTEGER PRIMARY KEY AUTOINCREMENT,
                        item_id                 INTEGER NOT NULL,
                        seller_username         TEXT    NOT NULL,
                        start_time              TEXT    NOT NULL,
                        end_time                TEXT    NOT NULL,
                        status                  TEXT    NOT NULL DEFAULT 'OPEN',
                        current_highest_bid     REAL    NOT NULL,
                        highest_bidder_username TEXT,
                        FOREIGN KEY (item_id) REFERENCES items(id)
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS bid_transactions (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT,
                        auction_id       INTEGER NOT NULL,
                        bidder_username  TEXT    NOT NULL,
                        bid_amount       REAL    NOT NULL,
                        bid_time         TEXT    NOT NULL,
                        FOREIGN KEY (auction_id) REFERENCES auctions(id)
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS auction_runtime_state (
                        auction_id       TEXT PRIMARY KEY,
                        seller_username  TEXT NOT NULL,
                        item_name        TEXT NOT NULL,
                        start_price      REAL NOT NULL,
                        current_price    REAL NOT NULL,
                        status           TEXT NOT NULL,
                        highest_bidder   TEXT,
                        end_time         INTEGER NOT NULL,
                        bid_count        INTEGER NOT NULL DEFAULT 0
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS auto_bid_settings (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT,
                        auction_id       INTEGER NOT NULL,
                        bidder_username  TEXT    NOT NULL,
                        max_bid          REAL    NOT NULL,
                        increment        REAL    NOT NULL,
                        is_active        INTEGER NOT NULL DEFAULT 1,
                        UNIQUE(auction_id, bidder_username)
                    )
                    """);

            st.execute("""
                    INSERT OR IGNORE INTO users (username, password_hash, email, phone, personal_id, role, created_at)
                    VALUES ('admin', '%s', 'admin@bidding.vnu.edu.vn', '', '', 'ADMIN',
                            strftime('%%s','now') * 1000)
                    """.formatted(PasswordHasher.hash("admin123")));

            System.out.println("[DB] Schema khoi tao thanh cong.");
        } catch (Exception e) {
            throw new RuntimeException("Loi khoi tao database: " + e.getMessage(), e);
        }
    }

    public static void resetAuctionRuntimeData() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM bid_transactions");
            st.executeUpdate("DELETE FROM auction_runtime_state");
            st.executeUpdate("DELETE FROM auto_bid_settings");
        } catch (Exception e) {
            throw new RuntimeException("Loi reset du lieu auction runtime: " + e.getMessage(), e);
        }
    }

    // Không nhận conn tham số nữa — tự mở/đóng connection riêng
    private static void ensureColumnExists(String tableName, String columnName, String definition) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return; // cột đã tồn tại
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Loi kiem tra cot " + tableName + "." + columnName, e);
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        } catch (Exception e) {
            throw new RuntimeException("Loi them cot " + tableName + "." + columnName, e);
        }
    }
}