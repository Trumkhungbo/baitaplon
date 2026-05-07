package com.bidding.server.model.item;

import com.bidding.server.enums.ItemType;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics() {
        super();
        setItemType(com.bidding.server.enums.ItemType.ELECTRONICS);
    }

    public Electronics(String name, String description, double startingPrice, String brand, int warrantyMonths) {
        super(name, description, startingPrice);
        setItemType(com.bidding.server.enums.ItemType.ELECTRONICS);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getItemDetails() {
        return "Electronics: " + getName() + " | Brand: " + brand + " | Warranty: " + warrantyMonths + " months";
    }

    private void setItemType(ItemType itemType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
