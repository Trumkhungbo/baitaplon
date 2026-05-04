package com.bidding.server.repository;

import com.bidding.common.enums.ItemType;
import com.bidding.common.model.item.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO extends BaseDAO {

    public Item save(Item item, String sellerUsername) {
        String sql = """
            INSERT INTO items (name, description, starting_price, item_type, seller_username,
                               artist, creation_year, brand, warranty_months, engine_type, mileage)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartingPrice());
            ps.setString(4, item.getItemType().name());
            ps.setString(5, sellerUsername);

            if (item instanceof Art a) {
                ps.setString(6, a.getArtist());
                ps.setInt(7, a.getCreationYear());
                ps.setNull(8, Types.TEXT); ps.setNull(9, Types.INTEGER);
                ps.setNull(10, Types.TEXT); ps.setNull(11, Types.INTEGER);
            } else if (item instanceof Electronics e) {
                ps.setNull(6, Types.TEXT); ps.setNull(7, Types.INTEGER);
                ps.setString(8, e.getBrand());
                ps.setInt(9, e.getWarrantyMonths());
                ps.setNull(10, Types.TEXT); ps.setNull(11, Types.INTEGER);
            } else if (item instanceof Vehicle v) {
                ps.setNull(6, Types.TEXT); ps.setNull(7, Types.INTEGER);
                ps.setNull(8, Types.TEXT); ps.setNull(9, Types.INTEGER);
                ps.setString(10, v.getEngineType());
                ps.setInt(11, v.getMileage());
            } else {
                for (int i = 6; i <= 11; i++) ps.setNull(i, Types.NULL);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getLong(1));
            }
            return item;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu item: " + e.getMessage(), e);
        }
    }

    public Item findById(long id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm item: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Item> findAll() {
        String sql = "SELECT * FROM items ORDER BY id";
        List<Item> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi list items: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Item> findBySeller(String sellerUsername) {
        String sql = "SELECT * FROM items WHERE seller_username = ?";
        List<Item> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sellerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm items: " + e.getMessage(), e);
        }
        return list;
    }

    private Item map(ResultSet rs) throws SQLException {
        ItemType type = ItemType.valueOf(rs.getString("item_type"));
        Item item = switch (type) {
            case ART -> {
                Art a = new Art();
                a.setArtist(rs.getString("artist"));
                a.setCreationYear(rs.getInt("creation_year"));
                yield a;
            }
            case ELECTRONICS -> {
                Electronics e = new Electronics();
                e.setBrand(rs.getString("brand"));
                e.setWarrantyMonths(rs.getInt("warranty_months"));
                yield e;
            }
            case VEHICLE -> {
                Vehicle v = new Vehicle();
                v.setEngineType(rs.getString("engine_type"));
                v.setMileage(rs.getInt("mileage"));
                yield v;
            }
            default -> new Art(); // fallback
        };
        item.setId(rs.getLong("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setItemType(type);
        return item;
    }
}