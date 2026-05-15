package com.bidding.server.repository;

import com.bidding.common.enums.UserRole;
import com.bidding.common.model.user.Admin;
import com.bidding.common.model.user.Bidder;
import com.bidding.common.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO extends BaseDAO {

    public User save(User user) {
        String sql = "INSERT INTO users (username, password_hash, email, phone, personal_id, role, balance, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getPersonalId());
            // BIDDER va SELLER deu hien thi la "USER" trong DB
            String roleStr = (user.getRole() == UserRole.ADMIN) ? "ADMIN" : "USER";
            ps.setString(6, roleStr);
            ps.setDouble(7, user instanceof Bidder b ? b.getBalance() : 0);
            ps.setLong(8, user.getCreatedAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Loi luu user: " + e.getMessage(), e);
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi tim user: " + e.getMessage(), e);
        }
        return null;
    }

    public User findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi tim user: " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> list = new ArrayList<>();
        try (Connection conn = getConn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi list users: " + e.getMessage(), e);
        }
        return list;
    }

    public void updateBalance(String username, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, newBalance);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Loi cap nhat balance: " + e.getMessage(), e);
        }
    }

    public void updatePasswordHash(String username, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, passwordHash);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Loi cap nhat password hash: " + e.getMessage(), e);
        }
    }

    public boolean delete(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Loi xoa user: " + e.getMessage(), e);
        }
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        String roleStr = rs.getString("role");
        UserRole role = "ADMIN".equals(roleStr) ? UserRole.ADMIN : UserRole.BIDDER;
        User user = switch (role) {
            case BIDDER -> {
                Bidder b = new Bidder();
                b.setBalance(rs.getDouble("balance"));
                yield b;
            }
            case ADMIN -> new Admin();
            default -> new Bidder();
        };
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPersonalId(rs.getString("personal_id"));
        user.setRole(role);
        user.setCreatedAt(rs.getLong("created_at"));
        return user;
    }
}