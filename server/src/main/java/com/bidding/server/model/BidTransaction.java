package com.bidding.server.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private long auctionId;
    private String bidderUsername;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction() {
        super();
        this.bidTime = LocalDateTime.now();
    }

    public BidTransaction(long auctionId, String bidderUsername, double bidAmount) {
        super();
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(long auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public void setBidderUsername(String bidderUsername) {
        this.bidderUsername = bidderUsername;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}
