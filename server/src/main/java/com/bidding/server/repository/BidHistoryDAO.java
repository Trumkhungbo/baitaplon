package com.bidding.server.repository;

import com.bidding.server.core.BidRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BidHistoryDAO extends BaseDAO {

    public void save(String auctionId, String bidderUsername, double bidAmount, long bidTime) {
        String sql = """
                INSERT INTO bid_transactions (auction_id, bidder_username, bid_amount, bid_time)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));
            ps.setString(2, bidderUsername);
            ps.setDouble(3, bidAmount);
            ps.setLong(4, bidTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save bid history", e);
        }
    }

    public List<BidRecord> findByAuctionId(String auctionId) {
        String sql = """
                SELECT bidder_username, bid_amount, bid_time
                FROM bid_transactions
                WHERE auction_id = ?
                ORDER BY bid_time, id
                """;
        List<BidRecord> records = new ArrayList<>();

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new BidRecord(
                            rs.getString("bidder_username"),
                            rs.getDouble("bid_amount"),
                            rs.getLong("bid_time")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load bid history", e);
        }

        return records;
    }

    public int countByAuctionId(String auctionId) {
        String sql = "SELECT COUNT(*) FROM bid_transactions WHERE auction_id = ?";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count bid history", e);
        }
    }
}
