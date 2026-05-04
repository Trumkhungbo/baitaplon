package com.bidding.common.model;

import java.time.LocalDateTime;

/** Ghi lại mỗi lần đặt giá */
public class BidTransaction {

    private long id;
    private long auctionId;
    private String bidderUsername;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction() {}

    public BidTransaction(long auctionId, String bidderUsername, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    @Override
    public String toString() {
        return String.format("Bid{auction=%d, bidder=%s, amount=%.2f, time=%s}",
                auctionId, bidderUsername, bidAmount, bidTime);
    }
}