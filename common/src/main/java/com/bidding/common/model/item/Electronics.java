package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;

public class Electronics extends Item {

    private String brand;
    private int warrantyMonths;

    public Electronics() {
        super();
        setItemType(ItemType.ELECTRONICS);
    }

    public Electronics(
            String name,
            double startingPrice,
            String imageUrl,
            String brand,
            int warrantyMonths
    ) {

        super(
                name,
                startingPrice,
                ItemType.ELECTRONICS,
                imageUrl
        );

        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {

        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand cannot be empty");
        }

        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {

        if (warrantyMonths < 0) {
            throw new IllegalArgumentException("Warranty months must be >= 0");
        }

        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getItemDetails() {

        return "Electronics{" +
                "name='" + getName() + '\'' +
                ", brand='" + brand + '\'' +
                ", warranty=" + warrantyMonths +
                " months, startingPrice=$" + getStartingPrice() +
                '}';
    }

    @Override
    public String toString() {
        return getItemDetails();
    }
}