package com.bidding.server.repository;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.Auction;
import com.bidding.common.model.BidTransaction;
import com.bidding.common.model.item.Item;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO extends BaseDAO {

    private final ItemDAO itemDAO = new ItemDAO();

    // ---- CREATE ----

    public Auction save(Auction auction) {
        String sql = """
            INSERT INTO auctions (item_id, seller_username, start_time, end_time,
                                  status, current_highest_bid, highest_bidder_username)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, auction.getItem().getId());
            ps.setString(2, auction.getSellerUsername());
            ps.setString(3, auction.getStartTime().toString());
            ps.setString(4, auction.getEndTime().toString());
            ps.setString(5, auction.getStatus().name());
            ps.setDouble(6, auction.getCurrentHighestBid());
            ps.setString(7, auction.getHighestBidderUsername());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) auction.setId(keys.getLong(1));
            }
            return auction;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu auction: " + e.getMessage(), e);
        }
    }

    // ---- READ ----

    public Auction findById(long id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm auction: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Auction> findAll() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY id";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi list auctions: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Auction> findByStatus(AuctionStatus status) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm auctions by status: " + e.getMessage(), e);
        }
        return list;
    }

    // ---- UPDATE ----

    public void updateBid(long auctionId, double newBid, String bidderUsername) {
        String sql = "UPDATE auctions SET current_highest_bid = ?, highest_bidder_username = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDouble(1, newBid);
            ps.setString(2, bidderUsername);
            ps.setLong(3, auctionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật bid: " + e.getMessage(), e);
        }
    }

    public void updateStatus(long auctionId, AuctionStatus status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, auctionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật status: " + e.getMessage(), e);
        }
    }

    // ---- BID HISTORY ----

    public void saveBidTransaction(BidTransaction bt) {
        String sql = """
            INSERT INTO bid_transactions (auction_id, bidder_username, bid_amount, bid_time)
            VALUES (?, ?, ?, ?)
        """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, bt.getAuctionId());
            ps.setString(2, bt.getBidderUsername());
            ps.setDouble(3, bt.getBidAmount());
            ps.setString(4, bt.getBidTime().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) bt.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu bid transaction: " + e.getMessage(), e);
        }
    }

    public List<BidTransaction> findBidHistory(long auctionId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bt = new BidTransaction();
                    bt.setId(rs.getLong("id"));
                    bt.setAuctionId(rs.getLong("auction_id"));
                    bt.setBidderUsername(rs.getString("bidder_username"));
                    bt.setBidAmount(rs.getDouble("bid_amount"));
                    bt.setBidTime(LocalDateTime.parse(rs.getString("bid_time")));
                    list.add(bt);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy bid history: " + e.getMessage(), e);
        }
        return list;
    }

    // ---- MAPPER ----

    private Auction map(ResultSet rs) throws SQLException {
        Auction a = new Auction();
        a.setId(rs.getLong("id"));
        a.setSellerUsername(rs.getString("seller_username"));
        a.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
        a.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
        a.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        a.setCurrentHighestBid(rs.getDouble("current_highest_bid"));
        a.setHighestBidderUsername(rs.getString("highest_bidder_username"));

        // Load item
        long itemId = rs.getLong("item_id");
        Item item = itemDAO.findById(itemId);
        a.setItem(item);

        return a;
    }
}