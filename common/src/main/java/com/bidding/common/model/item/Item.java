package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;
import com.bidding.common.model.Entity;

public abstract class Item extends Entity {

    private String name;
    private double startingPrice;
    private ItemType itemType;
    private String imageUrl;

    public Item() {
        super();
    }

    public Item(
            String name,
            double startingPrice,
            ItemType itemType,
            String imageUrl
    ) {
        super();

        this.name = name;
        this.startingPrice = startingPrice;
        this.itemType = itemType;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }

        this.name = name;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {

        if (startingPrice < 0) {
            throw new IllegalArgumentException("Starting price must be >= 0");
        }

        this.startingPrice = startingPrice;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {

        if (imageUrl == null) {
            imageUrl = "";
        }

        this.imageUrl = imageUrl;
    }
    public abstract String getItemDetails();

    @Override
    public String toString() {
        return "Item{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", startingPrice=" + startingPrice +
                ", itemType=" + itemType +
                ", imageUrl='" + imageUrl + '\'' +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}