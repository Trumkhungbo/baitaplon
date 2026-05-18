
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
                information1,
                information2,
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
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS
             )) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, getInformation1(item));
            ps.setString(4, getInformation2(item));
            ps.setDouble(5, item.getStartingPrice());
            ps.setString(6, item.getItemType().name());
            ps.setString(7, sellerUsername);
            ps.setString(8, item.getImageUrl());

            setTypeSpecificFields(ps, item);
            ps.setLong(15, item.getCreatedAt());

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

            ps.setString(9, a.getArtist());
            ps.setInt(10, a.getCreationYear());

        } else if (item instanceof Electronics e) {

            ps.setString(11, e.getBrand());
            ps.setInt(12, e.getWarrantyMonths());

        } else if (item instanceof Vehicle v) {

            ps.setString(13, v.getEngineType());
            ps.setInt(14, v.getMileage());
        }
    }

    private void clearTypeFields(PreparedStatement ps)
            throws SQLException {

        ps.setNull(9, Types.VARCHAR);
        ps.setNull(10, Types.INTEGER);

        ps.setNull(11, Types.VARCHAR);
        ps.setNull(12, Types.INTEGER);

        ps.setNull(13, Types.VARCHAR);
        ps.setNull(14, Types.INTEGER);
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

            art.setArtist(firstNonBlank(
                    rs.getString("artist"),
                    rs.getString("information1"),
                    "Unknown"
            ));
            art.setCreationYear(parseIntegerField(
                    rs,
                    "creation_year",
                    rs.getString("information2"),
                    java.time.Year.now().getValue()
            ));

            item = art;

        } else if (type == ItemType.ELECTRONICS) {

            Electronics electronics = new Electronics();

            electronics.setBrand(firstNonBlank(
                    rs.getString("brand"),
                    rs.getString("information1"),
                    "Unknown"
            ));
            electronics.setWarrantyMonths(parseIntegerField(
                    rs,
                    "warranty_months",
                    rs.getString("information2"),
                    0
            ));

            item = electronics;

        } else if (type == ItemType.VEHICLE) {

            Vehicle vehicle = new Vehicle();

            vehicle.setEngineType(firstNonBlank(
                    rs.getString("engine_type"),
                    rs.getString("information1"),
                    "Unknown"
            ));

            vehicle.setMileage(parseIntegerField(
                    rs,
                    "mileage",
                    rs.getString("information2"),
                    0
            ));

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

    private String getInformation1(Item item) {
        if (item instanceof Art art) {
            return art.getArtist();
        }
        if (item instanceof Electronics electronics) {
            return electronics.getBrand();
        }
        if (item instanceof Vehicle vehicle) {
            return vehicle.getEngineType();
        }
        return null;
    }

    private String getInformation2(Item item) {
        if (item instanceof Art art) {
            return String.valueOf(art.getCreationYear());
        }
        if (item instanceof Electronics electronics) {
            return String.valueOf(electronics.getWarrantyMonths());
        }
        if (item instanceof Vehicle vehicle) {
            return String.valueOf(vehicle.getMileage());
        }
        return null;
    }

    private String firstNonBlank(String primary, String fallback, String defaultValue) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return defaultValue;
    }

    private int parseIntegerField(ResultSet rs, String columnName, String fallback, int defaultValue) throws SQLException {
        int value = rs.getInt(columnName);
        if (!rs.wasNull()) {
            return value;
        }
        return fallback == null || fallback.isBlank() ? defaultValue : Integer.parseInt(fallback);
    }
    public String resolveInformation1(Item item) {
        return getInformation1(item);
    }

    public String resolveInformation2(Item item) {
        return getInformation2(item);
    }

}
