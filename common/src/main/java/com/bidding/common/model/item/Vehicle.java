package com.bidding.common.model;

public class Vehicle extends Item {
    private String engineType;
    private int mileage;

    public Vehicle(String name, String description, double startingPrice, String engineType, int mileage) {
        super(name, description, startingPrice);
        this.engineType = engineType;
        this.mileage = mileage;
    }

    // Getters & Setters
    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    @Override
    public String getItemDetails() {
        return "Vehicle: " + getName() + " | Engine: " + engineType + " | Mileage: " + mileage + " km | Starting Price: $" + getStartingPrice();
    }
}