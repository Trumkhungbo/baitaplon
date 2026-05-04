package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;

public class Vehicle extends Item {

    private String engineType;
    private int mileage;

    public Vehicle() { super(); }

    public Vehicle(String name, String description, double startingPrice,
                   String engineType, int mileage) {
        super(name, description, startingPrice, ItemType.VEHICLE);
        this.engineType = engineType;
        this.mileage = mileage;
    }

    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    @Override
    public String getItemDetails() {
        return String.format("Xe: %s | Động cơ: %s | Số km: %d | Giá: $%.2f",
                getName(), engineType, mileage, getStartingPrice());
    }
}