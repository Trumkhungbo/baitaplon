package com.bidding.common.model.item;


public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;

    public Item(String name, String description, double startingPrice) {
        super(); // Gọi constructor của Entity để lấy id và createdAt
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    // Getters và Setters cho name, description, startingPrice
    public String getName() { return name; }
    public double getStartingPrice() { return startingPrice; }

    // Polymorphism: Phương thức trừu tượng bắt buộc các lớp con phải ghi đè
    public abstract void printDetails();
}