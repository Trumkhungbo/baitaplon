package com.bidding.server.core;

public class Auction {

    private final String id;
    private final String sellerUsername;
    private final String itemName;
    private final double startPrice;

    private double currentPrice;
    private AuctionStatus status;
    private String highestBidder;
    private long endTime;

    public Auction(String id, String sellerUsername, String itemName, double startPrice, AuctionStatus status){
        this.id = id;
        this.sellerUsername = sellerUsername;
        this.itemName = itemName;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.status = status;
        this.highestBidder = null;
        this.endTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5 phút
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

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void extendEndTime(long extraTime) {
        this.endTime += extraTime;
    }
}
