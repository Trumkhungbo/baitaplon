package com.bidding.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TopUpRequestDAO extends BaseDAO {

    public long create(String username, double amount) {
        String sql = "INSERT INTO topup_requests (username, amount, status, requested_at) VALUES (?, ?, 'PENDING', ?)";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setDouble(2, amount);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi tao yeu cau nap tien: " + e.getMessage(), e);
        }
    }

    public List<TopUpRequest> findPending() {
        String sql = """
                SELECT tr.id, tr.username, tr.amount, tr.requested_at, u.balance, u.email, u.phone, u.personal_id
                FROM topup_requests tr
                JOIN users u ON u.username = tr.username
                WHERE tr.status = 'PENDING'
                ORDER BY tr.requested_at ASC
                """;
        List<TopUpRequest> requests = new ArrayList<>();
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                requests.add(new TopUpRequest(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getDouble("balance"),
                        rs.getDouble("amount"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("personal_id"),
                        rs.getLong("requested_at")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi lay yeu cau nap tien: " + e.getMessage(), e);
        }
        return requests;
    }

    public TopUpRequest findPendingById(long id) {
        String sql = """
                SELECT tr.id, tr.username, tr.amount, tr.requested_at, u.balance, u.email, u.phone, u.personal_id
                FROM topup_requests tr
                JOIN users u ON u.username = tr.username
                WHERE tr.id = ? AND tr.status = 'PENDING'
                """;
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TopUpRequest(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getDouble("balance"),
                            rs.getDouble("amount"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("personal_id"),
                            rs.getLong("requested_at")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi tim yeu cau nap tien: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean markApproved(long id) {
        String sql = "UPDATE topup_requests SET status = 'APPROVED', decided_at = ? WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Loi duyet yeu cau nap tien: " + e.getMessage(), e);
        }
    }

    public record TopUpRequest(
            long id,
            String username,
            double currentBalance,
            double amount,
            String email,
            String phone,
            String personalId,
            long requestedAt
    ) {
    }
}
