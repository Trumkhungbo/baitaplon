package com.auction.server.model;

public class Auction {
    private final String id;
    private final String itemName;
    private double currentPrice;
    private String status;

    public Auction(String id, String itemName, double currentPrice, String status) {
        this.id = id;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
