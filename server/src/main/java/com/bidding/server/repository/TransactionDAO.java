package com.bidding.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO extends BaseDAO {

    public void save(
            String username,
            String type,
            double amount,
            String description,
            String status,
            String relatedAuctionId,
            long createdAt
    ) {
        String sql = """
                INSERT INTO transactions (
                    username, type, amount, description, status, related_auction_id, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, description);
            ps.setString(5, status);
            ps.setString(6, relatedAuctionId);
            ps.setLong(7, createdAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save transaction", e);
        }
    }

    public List<TransactionRecord> findByUsername(String username) {
        String sql = """
                SELECT id, username, type, amount, description, status, related_auction_id, created_at
                FROM transactions
                WHERE username = ?
                ORDER BY created_at DESC, id DESC
                """;

        List<TransactionRecord> records = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new TransactionRecord(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("type"),
                            rs.getDouble("amount"),
                            rs.getString("description"),
                            rs.getString("status"),
                            rs.getString("related_auction_id"),
                            rs.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions", e);
        }
        return records;
    }

    public record TransactionRecord(
            long id,
            String username,
            String type,
            double amount,
            String description,
            String status,
            String relatedAuctionId,
            long createdAt
    ) {
    }
}
