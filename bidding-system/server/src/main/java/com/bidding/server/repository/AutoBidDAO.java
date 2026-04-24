package com.bidding.server.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO extends BaseDAO {

    // =========================
    // CREATE / UPDATE (UPSERT)
    // =========================
    public void registerAutoBid(long auctionId,
                                long bidderId,
                                double maxBid,
                                double increment) {

        final String sql = """
                INSERT OR REPLACE INTO auto_bid_settings
                (auction_id, bidder_id, max_bid, increment, is_active)
                VALUES (?, ?, ?, ?, 1)
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            ps.setLong(2, bidderId);
            ps.setDouble(3, maxBid);
            ps.setDouble(4, increment);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error registering auto bid", e);
        }
    }

    // =========================
    // DISABLE
    // =========================
    public boolean disableAutoBid(long auctionId, long bidderId) {

        final String sql = """
                UPDATE auto_bid_settings
                SET is_active = 0
                WHERE auction_id = ? AND bidder_id = ?
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            ps.setLong(2, bidderId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error disabling auto bid", e);
        }
    }

    // =========================
    // GET active auto bids of auction
    // =========================
    public List<AutoBid> findActiveByAuction(long auctionId) {

        final String sql = """
                SELECT * FROM auto_bid_settings
                WHERE auction_id = ? AND is_active = 1
                ORDER BY max_bid DESC
                """;

        List<AutoBid> list = new ArrayList<>();

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setLong(1, auctionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching auto bids", e);
        }

        return list;
    }

    // =========================
    // GET 1 auto bid
    // =========================
    public AutoBid findOne(long auctionId, long bidderId) {

        final String sql = """
                SELECT * FROM auto_bid_settings
                WHERE auction_id = ? AND bidder_id = ?
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            ps.setLong(2, bidderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding auto bid", e);
        }

        return null;
    }

    // =========================
    // MAPPER
    // =========================
    private AutoBid map(ResultSet rs) throws SQLException {
        AutoBid a = new AutoBid();

        a.setAuctionId(rs.getLong("auction_id"));
        a.setBidderId(rs.getLong("bidder_id"));
        a.setMaxBid(rs.getDouble("max_bid"));
        a.setIncrement(rs.getDouble("increment"));
        a.setActive(rs.getInt("is_active") == 1);

        return a;
    }
}