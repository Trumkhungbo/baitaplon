package com.bidding.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.core.Auction;

public class AuctionStateDAO extends BaseDAO {

    public AuctionStateSnapshot findByAuctionId(String auctionId) {
        String sql = """
                SELECT auction_id, seller_username, item_name, start_price,
                       current_price, status, highest_bidder, end_time, start_time, duration_minutes, bid_count
                FROM auction_runtime_state
                WHERE auction_id = ?
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new AuctionStateSnapshot(
                        rs.getString("auction_id"),
                        rs.getString("seller_username"),
                        rs.getString("item_name"),
                        rs.getDouble("start_price"),
                        rs.getDouble("current_price"),
                        parseStatus(rs.getString("status")),
                        rs.getString("highest_bidder"),
                        rs.getLong("end_time"),
                        rs.getLong("start_time"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("bid_count")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load auction state", e);
        }
    }

    public List<AuctionStateSnapshot> findAll() {
        String sql = """
                SELECT auction_id, seller_username, item_name, start_price,
                       current_price, status, highest_bidder, end_time, start_time, duration_minutes, bid_count
                FROM auction_runtime_state
                ORDER BY CAST(auction_id AS INTEGER)
                """;
        List<AuctionStateSnapshot> snapshots = new ArrayList<>();

        try (Connection conn = getConn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                snapshots.add(new AuctionStateSnapshot(
                        rs.getString("auction_id"),
                        rs.getString("seller_username"),
                        rs.getString("item_name"),
                        rs.getDouble("start_price"),
                        rs.getDouble("current_price"),
                        AuctionStatus.valueOf(rs.getString("status")),
                        rs.getString("highest_bidder"),
                        rs.getLong("end_time"),
                        rs.getLong("start_time"),
                        rs.getInt("duration_minutes"),
                        rs.getInt("bid_count")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load auction states", e);
        }

        return snapshots;
    }

    public void upsert(Auction auction, int bidCount) {
        String sql = """
                INSERT INTO auction_runtime_state (
                    auction_id, seller_username, item_name, start_price,
                    current_price, status, highest_bidder, end_time, start_time, duration_minutes, bid_count
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(auction_id) DO UPDATE SET
                    seller_username = excluded.seller_username,
                    item_name = excluded.item_name,
                    start_price = excluded.start_price,
                    current_price = excluded.current_price,
                    status = excluded.status,
                    highest_bidder = excluded.highest_bidder,
                    end_time = excluded.end_time,
                    start_time = excluded.start_time,
                    duration_minutes = excluded.duration_minutes,
                    bid_count = excluded.bid_count
                """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId());
            ps.setString(2, auction.getSellerUsername());
            ps.setString(3, auction.getItemName());
            ps.setDouble(4, auction.getStartPrice());
            ps.setDouble(5, auction.getCurrentPrice());
            ps.setString(6, auction.getStatus().name());
            ps.setString(7, auction.getHighestBidder());
            ps.setLong(8, auction.getEndTime());
            ps.setLong(9, auction.getStartTimeMillis());
            ps.setInt(10, auction.getDurationMinutes());
            ps.setInt(11, bidCount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save auction state", e);
        }
    }

    public record AuctionStateSnapshot(
            String auctionId,
            String sellerUsername,
            String itemName,
            double startPrice,
            double currentPrice,
            AuctionStatus status,
            String highestBidder,
            long endTimeMillis,
            long startTimeMillis,
            int durationMinutes,
            int bidCount
    ) {
    }
}
