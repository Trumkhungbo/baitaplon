package com.bidding.server.model;

public class Auction {

    private final String id;
    private final String sellerUsername;
    private final String itemName;
    private final double startPrice;

    private double currentPrice;
    private AuctionStatus status;
    private String highestBidder;

    public Auction(String id, String sellerUsername, String itemName, double startPrice, AuctionStatus status) {
        this.id = id;
        this.sellerUsername = sellerUsername;
        this.itemName = itemName;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.status = status;
        this.highestBidder = null;
    }

    public String getId() {
        return id;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public String getItemName() {
        return itemName;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(String highestBidder) {
        this.highestBidder = highestBidder;
    }
}