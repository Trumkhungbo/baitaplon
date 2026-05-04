package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract base class cho các loại sản phẩm đấu giá.
 * Dùng @JsonTypeInfo để Jackson biết serialize/deserialize đúng subclass.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "itemType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Art.class,         name = "ART"),
    @JsonSubTypes.Type(value = Electronics.class, name = "ELECTRONICS"),
    @JsonSubTypes.Type(value = Vehicle.class,     name = "VEHICLE")
})
public abstract class Item {

    private long id;
    private String name;
    private String description;
    private double startingPrice;
    private ItemType itemType;

    // Jackson cần constructor không tham số
    protected Item() {}

    protected Item(String name, String description, double startingPrice, ItemType itemType) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.itemType = itemType;
    }

    // ---- Getters & Setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    /** Polymorphism: mỗi subclass trả về thông tin chi tiết riêng */
    public abstract String getItemDetails();

    @Override
    public String toString() {
        return String.format("[%s] %s — Giá khởi điểm: $%.2f", itemType, name, startingPrice);
    }
}