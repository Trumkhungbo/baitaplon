package com.bidding.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.enums.ItemType;
import com.bidding.common.model.Auction;
import com.bidding.common.model.BidTransaction;
import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Electronics;
import com.bidding.common.model.item.Item;
import com.bidding.common.model.item.Vehicle;

public class AuctionDAO extends BaseDAO {

    private final ItemDAO itemDAO;

    public AuctionDAO(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    // ---- CREATE ----

    public Auction save(Auction auction) {
        String sql = """
            INSERT INTO auctions (item_id, seller_username, start_time, end_time,
                                  status, current_highest_bid, highest_bidder_username)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        // ✅ FIX #1: Đóng Connection trong try-with-resources
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, auction.getItem().getId());
            ps.setString(2, auction.getSellerUsername());
            ps.setTimestamp(3, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
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
        String sql = """
            SELECT a.*,
                   i.id AS i_id, i.name AS i_name, i.description AS i_description,
                   i.starting_price AS i_starting_price, i.item_type AS i_item_type,
                   i.image_url AS i_image_url,
                   i.artist AS i_artist, i.creation_year AS i_creation_year,
                   i.brand AS i_brand, i.warranty_months AS i_warranty_months,
                   i.engine_type AS i_engine_type, i.mileage AS i_mileage
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.id = ?
        """;
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
        String sql = """
            SELECT a.*,
                   i.id AS i_id, i.name AS i_name, i.description AS i_description,
                   i.starting_price AS i_starting_price, i.item_type AS i_item_type,
                   i.image_url AS i_image_url,
                   i.artist AS i_artist, i.creation_year AS i_creation_year,
                   i.brand AS i_brand, i.warranty_months AS i_warranty_months,
                   i.engine_type AS i_engine_type, i.mileage AS i_mileage
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            ORDER BY a.id
        """;
        try (Connection conn = getConn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi list auctions: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Auction> findByStatus(AuctionStatus status) {
        List<Auction> list = new ArrayList<>();
        String sql = """
            SELECT a.*,
                   i.id AS i_id, i.name AS i_name, i.description AS i_description,
                   i.starting_price AS i_starting_price, i.item_type AS i_item_type,
                   i.image_url AS i_image_url,
                   i.artist AS i_artist, i.creation_year AS i_creation_year,
                   i.brand AS i_brand, i.warranty_months AS i_warranty_months,
                   i.engine_type AS i_engine_type, i.mileage AS i_mileage
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.status = ?
        """;
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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

    public void updateStatus(long auctionId, AuctionStatus status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setLong(2, auctionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật status: " + e.getMessage(), e);
        }
    }

    // ---- BID (ATOMIC) ----

    /**
     * Gộp updateBid + saveBidTransaction vào một transaction atomic.
     * Nếu một trong hai thất bại, toàn bộ được rollback.
     */
    public BidTransaction placeBid(long auctionId, double newBid,
                                   String bidderUsername, BidTransaction bt) {
        String updateSql = """
            UPDATE auctions
            SET current_highest_bid = ?, highest_bidder_username = ?
            WHERE id = ?
        """;
        String insertSql = """
            INSERT INTO bid_transactions (auction_id, bidder_username, bid_amount, bid_time)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                // 1. Cập nhật bid cao nhất
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setDouble(1, newBid);
                    ps.setString(2, bidderUsername);
                    ps.setLong(3, auctionId);
                    ps.executeUpdate();
                }

                // 2. Lưu lịch sử bid
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, bt.getAuctionId());
                    ps.setString(2, bt.getBidderUsername());
                    ps.setDouble(3, bt.getBidAmount());
                    ps.setTimestamp(4, Timestamp.valueOf(bt.getBidTime()));
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) bt.setId(keys.getLong(1));
                    }
                }

                conn.commit();
                return bt;

            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Lỗi đặt bid (đã rollback): " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kết nối khi đặt bid: " + e.getMessage(), e);
        }
    }

    // ---- BID HISTORY ----

    public List<BidTransaction> findBidHistory(long auctionId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bt = new BidTransaction();
                    bt.setId(rs.getLong("id"));
                    bt.setAuctionId(rs.getLong("auction_id"));
                    bt.setBidderUsername(rs.getString("bidder_username"));
                    bt.setBidAmount(rs.getDouble("bid_amount"));
                    // ✅ FIX #3: Dùng Timestamp
                    bt.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
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
        a.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        a.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        a.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        a.setCurrentHighestBid(rs.getDouble("current_highest_bid"));
        a.setHighestBidderUsername(rs.getString("highest_bidder_username"));

        ItemType itemType = ItemType.valueOf(rs.getString("i_item_type"));
        Item item = switch (itemType) {
            case ART -> {
                Art art = new Art();
                art.setArtist(rs.getString("i_artist"));
                art.setCreationYear(rs.getInt("i_creation_year"));
                yield art;
            }
            case ELECTRONICS -> {
                Electronics elec = new Electronics();
                elec.setBrand(rs.getString("i_brand"));
                elec.setWarrantyMonths(rs.getInt("i_warranty_months"));
                yield elec;
            }
            case VEHICLE -> {
                Vehicle v = new Vehicle();
                v.setEngineType(rs.getString("i_engine_type"));
                v.setMileage(rs.getInt("i_mileage"));
                yield v;
            }
            default -> new Art();
        };
        item.setId(rs.getLong("i_id"));
        item.setName(rs.getString("i_name"));
        item.setDescription(rs.getString("i_description"));
        item.setStartingPrice(rs.getDouble("i_starting_price"));
        item.setItemType(itemType);
        item.setImageUrl(rs.getString("i_image_url")); // cần thêm field + cột DB
        a.setItem(item);

        return a;
    }
}