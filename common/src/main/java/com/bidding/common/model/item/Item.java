package com.bidding.common.model;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;

    public Item(String name, String description, double startingPrice) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }

    // Polymorphism
    public abstract String getItemDetails();
}