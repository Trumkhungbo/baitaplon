
package com.bidding.server.model.item;

import com.bidding.common.enums.ItemType;

public abstract class Item extends com.bidding.server.model.Entity {
    private String name;
    private String description;
    private double startingPrice;
    private ItemType itemType;
    private String imageUrl;

    public Item() {
        super();
    }

    public Item(String name, String description, double startingPrice) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
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
        this.imageUrl = imageUrl;
    }

    public abstract String getItemDetails();
}