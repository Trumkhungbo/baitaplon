package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;

public class Electronics extends Item {

    private String brand;
    private int warrantyMonths;

    public Electronics() { super(); }

    public Electronics(String name, String description, double startingPrice,
                       String brand, int warrantyMonths) {
        super(name, description, startingPrice, ItemType.ELECTRONICS);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    @Override
    public String getItemDetails() {
        return String.format("Điện tử: %s | Hãng: %s | Bảo hành: %d tháng | Giá: $%.2f",
                getName(), brand, warrantyMonths, getStartingPrice());
    }
}