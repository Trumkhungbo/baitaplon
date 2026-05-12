package com.bidding.server.database;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Tạo schema database khi server khởi động lần đầu.
 * Dùng CREATE TABLE IF NOT EXISTS để an toàn khi chạy lại.
 */
public class DatabaseInitializer {

    public static void initialize() {
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement st = conn.createStatement()) {

            // Bảng users 
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

            // Bảng items
            st.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    name            TEXT    NOT NULL,
                    description     TEXT,
                    starting_price  REAL    NOT NULL,
                    item_type       TEXT    NOT NULL,
                    seller_username TEXT    NOT NULL,
                    -- Art
                    artist          TEXT,
                    creation_year   INTEGER,
                    -- Electronics
                    brand           TEXT,
                    warranty_months INTEGER,
                    -- Vehicle
                    engine_type     TEXT,
                    mileage         INTEGER,
                    image_url       TEXT
                )
            """);

            // Bảng auctions
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

            // Bảng bid_transactions
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

            // Bảng auto_bid_settings
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

            // Seed admin mặc định nếu chưa có
            st.execute("""
                INSERT OR IGNORE INTO users (username, password_hash, email, phone, personal_id, role, created_at)
                VALUES ('admin', 'admin123', 'admin@bidding.vnu.edu.vn', '0987654321', '123123123123', 'ADMIN',
                        strftime('%s','now') * 1000)
            """);
            System.out.println("[DB] Schema khởi tạo thành công.");

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khởi tạo database: " + e.getMessage(), e);
        }
    }
}