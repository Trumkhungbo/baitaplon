package com.bidding.common.model.item;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics(String name, String description, double startingPrice, String brand, int warrantyMonths) {
        super(name, description, startingPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getItemDetails() {
        return "Electronics: " + getName() + " | Brand: " + brand + " | Warranty: " + warrantyMonths + " months";
    }
}