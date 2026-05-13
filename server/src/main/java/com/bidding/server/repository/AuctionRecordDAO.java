package com.bidding.server.repository;

import com.bidding.common.enums.ItemType;
import com.bidding.common.model.item.Art;
import com.bidding.server.core.Auction;
import com.bidding.server.core.AuctionStatus;

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

    public void save(String auctionId, String sellerUsername, String itemName, double startPrice, long endTime, AuctionStatus status) {
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
                    status, current_highest_bid, highest_bidder_username
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            ps.setLong(1, Long.parseLong(auctionId));
            ps.setLong(2, item.getId());
            ps.setString(3, sellerUsername);
            ps.setString(4, String.valueOf(now));
            ps.setString(5, String.valueOf(endTime));
            ps.setString(6, status.name());
            ps.setDouble(7, startPrice);
            ps.setString(8, null);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save auction record", e);
        }
    }

    public void updateState(Auction auction) {
        String sql = """
                UPDATE auctions
                SET end_time = ?,
                    status = ?,
                    current_highest_bid = ?,
                    highest_bidder_username = ?
                WHERE id = ?
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(auction.getEndTime()));
            ps.setString(2, auction.getStatus().name());
            ps.setDouble(3, auction.getCurrentPrice());
            ps.setString(4, auction.getHighestBidder());
            ps.setLong(5, Long.parseLong(auction.getId()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update auction record", e);
        }
    }
}
