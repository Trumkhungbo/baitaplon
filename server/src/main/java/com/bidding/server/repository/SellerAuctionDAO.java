package com.bidding.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SellerAuctionDAO extends BaseDAO {

    public void deleteBidHistoryByAuctionId(String auctionId) {
        String sql = "DELETE FROM bid_transactions WHERE auction_id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete bid history", e);
        }
    }

    public void deleteAutoBidByAuctionId(String auctionId) {
        String sql = "DELETE FROM auto_bid_settings WHERE auction_id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(auctionId));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete auto-bid settings", e);
        }
    }
}
