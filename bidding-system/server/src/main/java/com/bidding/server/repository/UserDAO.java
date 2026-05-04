package com.bidding.server.repository;

import com.bidding.common.enums.UserRole;
import com.bidding.common.model.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO extends BaseDAO {

    // ---- CREATE ----

    public User save(User user) {
        String sql = "INSERT INTO users (username, password_hash, email, full_name, role, balance, rating, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole().name());
            ps.setDouble(6, user instanceof Bidder ? ((Bidder) user).getBalance() : 0);
            ps.setDouble(7, user instanceof Seller ? ((Seller) user).getRating() : 0);
            ps.setLong(8, user.getCreatedAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getLong(1));
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu user: " + e.getMessage(), e);
        }
    }

    // ---- READ ----

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm user: " + e.getMessage(), e);
        }
        return null;
    }

    public User findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm user: " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi list users: " + e.getMessage(), e);
        }
        return list;
    }

    // ---- UPDATE ----

    public void updateBalance(String username, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật balance: " + e.getMessage(), e);
        }
    }

    // ---- DELETE ----

    public boolean delete(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa user: " + e.getMessage(), e);
        }
    }

    // ---- CHECK ----

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
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
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    // ---- MAPPER ----

    private User map(ResultSet rs) throws SQLException {
        UserRole role = UserRole.valueOf(rs.getString("role"));
        User user;
        switch (role) {
            case BIDDER:
                Bidder b = new Bidder();
                b.setBalance(rs.getDouble("balance"));
                user = b;
                break;
            case SELLER:
                Seller s = new Seller();
                s.setRating(rs.getDouble("rating"));
                user = s;
                break;
            case ADMIN:
                user = new Admin();
                break;
            default:
                throw new IllegalStateException("Unexpected role: " + role);
        }
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(role);
        user.setCreatedAt(rs.getLong("created_at"));
        return user;
    }
}