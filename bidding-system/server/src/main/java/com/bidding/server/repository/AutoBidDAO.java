package com.bidding.server.repository;

import com.bidding.common.model.AutoBid;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO extends BaseDAO {

    /** Tạo mới hoặc cập nhật auto-bid (upsert) */
    public void upsert(AutoBid ab) {
        String sql = "INSERT INTO auto_bid_settings (auction_id, bidder_username, max_bid, increment, is_active) "
                + "VALUES (?, ?, ?, ?, 1) "
                + "ON CONFLICT(auction_id, bidder_username) "
                + "DO UPDATE SET max_bid = excluded.max_bid, "
                + "increment = excluded.increment, "
                + "is_active = 1";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, ab.getAuctionId());
            ps.setString(2, ab.getBidderUsername());
            ps.setDouble(3, ab.getMaxBid());
            ps.setDouble(4, ab.getIncrement());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi upsert auto bid: " + e.getMessage(), e);
        }
    }

    public boolean disable(long auctionId, String bidderUsername) {
        String sql = "UPDATE auto_bid_settings SET is_active = 0 WHERE auction_id = ? AND bidder_username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ps.setString(2, bidderUsername);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi disable auto bid: " + e.getMessage(), e);
        }
    }

    public List<AutoBid> findActiveByAuction(long auctionId) {
        String sql = "SELECT * FROM auto_bid_settings "
                + "WHERE auction_id = ? AND is_active = 1 "
                + "ORDER BY max_bid DESC";
        List<AutoBid> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy auto bids: " + e.getMessage(), e);
        }
        return list;
    }

    public AutoBid findOne(long auctionId, String bidderUsername) {
        String sql = "SELECT * FROM auto_bid_settings WHERE auction_id = ? AND bidder_username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ps.setString(2, bidderUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm auto bid: " + e.getMessage(), e);
        }
        return null;
    }

    private AutoBid map(ResultSet rs) throws SQLException {
        AutoBid ab = new AutoBid();
        ab.setId(rs.getLong("id"));
        ab.setAuctionId(rs.getLong("auction_id"));
        ab.setBidderUsername(rs.getString("bidder_username"));
        ab.setMaxBid(rs.getDouble("max_bid"));
        ab.setIncrement(rs.getDouble("increment"));
        ab.setActive(rs.getInt("is_active") == 1);
        return ab;
    }
}