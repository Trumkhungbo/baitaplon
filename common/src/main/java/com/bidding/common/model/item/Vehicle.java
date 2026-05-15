package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;

public class Vehicle extends Item {

    private String engineType;
    private int mileage;

    public Vehicle() {
        super();
        setItemType(ItemType.VEHICLE);
    }

    public Vehicle(
            String name,
            String description,
            double startingPrice,
            String imageUrl,
            String engineType,
            int mileage
    ) {

        super(
                name,
                description,
                startingPrice,
                ItemType.VEHICLE,
                imageUrl
        );

        this.engineType = engineType;
        this.mileage = mileage;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {

        if (engineType == null || engineType.trim().isEmpty()) {
            throw new IllegalArgumentException("Engine type cannot be empty");
        }

        this.engineType = engineType;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {

        if (mileage < 0) {
            throw new IllegalArgumentException("Mileage must be >= 0");
        }

        this.mileage = mileage;
    }

    @Override
    public String getItemDetails() {

        return "Vehicle{" +
                "name='" + getName() + '\'' +
                ", engineType='" + engineType + '\'' +
                ", mileage=" + mileage +
                " km, startingPrice=$" + getStartingPrice() +
                '}';
    }

    @Override
    public String toString() {
        return getItemDetails();
    }
}