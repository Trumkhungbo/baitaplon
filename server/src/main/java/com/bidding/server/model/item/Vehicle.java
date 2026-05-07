package com.bidding.server.model.item;

public class Vehicle extends Item {
    private String engineType;
    private int mileage;

    public Vehicle() {
        super();
        setItemType(com.bidding.common.enums.ItemType.VEHICLE);
    }

    public Vehicle(String name, String description, double startingPrice, String engineType, int mileage) {
        super(name, description, startingPrice);
        setItemType(com.bidding.common.enums.ItemType.VEHICLE);
        this.engineType = engineType;
        this.mileage = mileage;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    @Override
    public String getItemDetails() {
        return "Vehicle: " + getName() + " | Engine: " + engineType + " | Mileage: " + mileage + " km | Starting Price: $" + getStartingPrice();
    }
}
