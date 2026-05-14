package com.bidding.server.repository;

import com.bidding.common.enums.ItemType;
import com.bidding.common.model.item.Art;
import com.bidding.server.core.Auction;
import com.bidding.common.enums.AuctionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuctionRecordDAO extends BaseDAO {

    private final ItemDAO itemDAO = new ItemDAO();

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

    public void save(String auctionId, String sellerUsername, String itemName, double startPrice, long startTimeMillis, int durationMinutes, AuctionStatus status) {
        Art item = new Art();
        item.setName(itemName);
        item.setDescription("");
        item.setStartingPrice(startPrice);
        item.setItemType(ItemType.OTHER);
        item.setImageUrl(null);
        itemDAO.save(item, sellerUsername);

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
            ps.setLong(2, item.getId());
            ps.setString(3, sellerUsername);
            ps.setString(4, String.valueOf(startTimeMillis));
            ps.setString(5, String.valueOf(startTimeMillis + (durationMinutes * 60_000L)));
            ps.setInt(6, durationMinutes);
            ps.setString(7, status.name());
            ps.setDouble(8, startPrice);
            ps.setString(9, null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save auction record", e);
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
            ps.setString(1, String.valueOf(auction.getStartTimeMillis()));
            ps.setString(2, String.valueOf(auction.getEndTime()));
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
}
