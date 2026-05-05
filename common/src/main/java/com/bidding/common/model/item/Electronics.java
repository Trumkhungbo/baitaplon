package com.bidding.common.model.item;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics() {
        super();
        setItemType(com.bidding.common.enums.ItemType.ELECTRONICS);
    }

    public Electronics(String name, String description, double startingPrice, String brand, int warrantyMonths) {
        super(name, description, startingPrice);
        setItemType(com.bidding.common.enums.ItemType.ELECTRONICS);
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
}
