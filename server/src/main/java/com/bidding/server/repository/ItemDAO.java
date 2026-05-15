
package com.bidding.server.repository;

import com.bidding.common.enums.ItemType;
import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Electronics;
import com.bidding.common.model.item.Item;
import com.bidding.common.model.item.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO extends BaseDAO {

    public Item save(Item item, String sellerUsername) {

        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        String sql = """
            INSERT INTO items (
                name,
                description,
                starting_price,
                item_type,
                seller_username,
                image_url,
                artist,
                creation_year,
                brand,
                warranty_months,
                engine_type,
                mileage,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartingPrice());
            ps.setString(4, item.getItemType().name());
            ps.setString(5, sellerUsername);
            ps.setString(6, item.getImageUrl());

            setTypeSpecificFields(ps, item);
            ps.setLong(13, item.getCreatedAt());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {

                if (keys.next()) {
                    item.setId(keys.getLong(1));
                }
            }

            return item;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save item: " + e.getMessage(),
                    e
            );
        }
    }

    public Item findById(long id) {

        String sql = "SELECT * FROM items WHERE id = ?";

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return map(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find item by id",
                    e
            );
        }

        return null;
    }

    public List<Item> findAll() {

        String sql = "SELECT * FROM items ORDER BY id DESC";

        List<Item> items = new ArrayList<>();

        try (Connection conn = getConn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                items.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to load items",
                    e
            );
        }

        return items;
    }

    public List<Item> findBySeller(String sellerUsername) {

        String sql = """
            SELECT * FROM items
            WHERE seller_username = ?
            ORDER BY id DESC
        """;

        List<Item> items = new ArrayList<>();

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sellerUsername);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    items.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find seller items",
                    e
            );
        }

        return items;
    }

    public List<Item> findByType(ItemType type) {

        String sql = """
            SELECT * FROM items
            WHERE item_type = ?
            ORDER BY id DESC
        """;

        List<Item> items = new ArrayList<>();

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type.name());

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    items.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to find items by type",
                    e
            );
        }

        return items;
    }

    private void setTypeSpecificFields(
            PreparedStatement ps,
            Item item
    ) throws SQLException {

        clearTypeFields(ps);

        if (item instanceof Art a) {

            ps.setString(7, a.getArtist());
            ps.setInt(8, a.getCreationYear());

        } else if (item instanceof Electronics e) {

            ps.setString(9, e.getBrand());
            ps.setInt(10, e.getWarrantyMonths());

        } else if (item instanceof Vehicle v) {

            ps.setString(11, v.getEngineType());
            ps.setInt(12, v.getMileage());
        }
    }

    private void clearTypeFields(PreparedStatement ps)
            throws SQLException {

        ps.setNull(7, Types.VARCHAR);
        ps.setNull(8, Types.INTEGER);

        ps.setNull(9, Types.VARCHAR);
        ps.setNull(10, Types.INTEGER);

        ps.setNull(11, Types.VARCHAR);
        ps.setNull(12, Types.INTEGER);
    }

    private Item map(ResultSet rs) throws SQLException {

        String rawType = rs.getString("item_type");

        if (rawType == null) {
            throw new RuntimeException("Item type is null");
        }

        ItemType type;

        try {
            type = ItemType.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Unknown item type: " + rawType
            );
        }

        Item item;

        if (type == ItemType.ART) {

            Art art = new Art();

            art.setArtist(rs.getString("artist"));
            art.setCreationYear(rs.getInt("creation_year"));

            item = art;

        } else if (type == ItemType.ELECTRONICS) {

            Electronics electronics = new Electronics();

            electronics.setBrand(rs.getString("brand"));
            electronics.setWarrantyMonths(
                    rs.getInt("warranty_months")
            );

            item = electronics;

        } else if (type == ItemType.VEHICLE) {

            Vehicle vehicle = new Vehicle();

            vehicle.setEngineType(
                    rs.getString("engine_type")
            );

            vehicle.setMileage(
                    rs.getInt("mileage")
            );

            item = vehicle;

        } else {
            throw new RuntimeException(
                    "Unsupported item type: " + type
            );
        }

        item.setId(rs.getLong("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setItemType(type);
        item.setImageUrl(rs.getString("image_url"));

        return item;
    }
}
