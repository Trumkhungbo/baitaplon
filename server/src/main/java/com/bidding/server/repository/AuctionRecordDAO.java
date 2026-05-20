package com.bidding.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.core.Auction;

public class AuctionRecordDAO extends BaseDAO {

    public boolean existsById(String auctionId) {
        String sql = "SELECT 1 FROM auctions WHERE id = ?";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check auction existence", e);
        }
    }

    public long findMaxAuctionId() {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM auctions";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get max auction id", e);
        }
    }

    public long findItemIdByAuctionId(String auctionId) {
        String sql = "SELECT item_id FROM auctions WHERE id = ?";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("item_id") : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find item id by auction id", e);
        }
    }

    public void save(String auctionId, long itemId, String sellerUsername, double startPrice, long startTimeMillis, long endTimeMillis, int durationMinutes, AuctionStatus status) {
        String sql = """
                INSERT INTO auctions (
                    id, item_id, seller_username, start_time, end_time,
                    duration_minutes, status, current_highest_bid, highest_bidder_username
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));
            ps.setLong(2, itemId);
            ps.setString(3, sellerUsername);
            ps.setLong(4, startTimeMillis);
            ps.setLong(5, endTimeMillis);
            ps.setInt(6, durationMinutes);
            ps.setString(7, status.name());
            ps.setDouble(8, startPrice);
            ps.setString(9, null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save auction record", e);
        }
    }

    public void save(String auctionId, String sellerUsername, String itemName, double startPrice, long startTimeMillis, int durationMinutes, AuctionStatus status) {
        throw new UnsupportedOperationException("Use save(...) with itemId");
    }

    public boolean approvePendingAuction(String auctionId) {
        String sql = """
                UPDATE auctions
                SET status = 'OPEN'
                WHERE id = ? AND status = 'PENDING'
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to approve auction", e);
        }
    }

    public void updateState(Auction auction) {
        String sql = """
                UPDATE auctions
                SET start_time = ?,
                    end_time = ?,
                    duration_minutes = ?,
                    status = ?,
                    current_highest_bid = ?,
                    highest_bidder_username = ?
                WHERE id = ?
                """;

        try (Connection conn = getConn();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auction.getStartTimeMillis());
            ps.setLong(2, auction.getEndTime());
            ps.setInt(3, auction.getDurationMinutes());
            ps.setString(4, auction.getStatus().name());
            ps.setDouble(5, auction.getCurrentPrice());
            ps.setString(6, auction.getHighestBidder());
            ps.setLong(7, Long.parseLong(auction.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update auction record", e);
        }
    }

    public void updateListing(String auctionId, long itemId, String sellerUsername, double startPrice, long startTimeMillis, long endTimeMillis, int durationMinutes, AuctionStatus status) {
        String sql = """
                UPDATE auctions
                SET item_id = ?,
                    seller_username = ?,
                    start_time = ?,
                    end_time = ?,
                    duration_minutes = ?,
                    status = ?,
                    current_highest_bid = ?,
                    highest_bidder_username = NULL
                WHERE id = ?
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            ps.setString(2, sellerUsername);
            ps.setLong(3, startTimeMillis);
            ps.setLong(4, endTimeMillis);
            ps.setInt(5, durationMinutes);
            ps.setString(6, status.name());
            ps.setDouble(7, startPrice);
            ps.setLong(8, Long.parseLong(auctionId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update auction listing", e);
        }
    }

    public void deleteByAuctionId(String auctionId) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete auction record", e);
        }
    }
}
