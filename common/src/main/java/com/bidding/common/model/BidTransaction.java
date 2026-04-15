package com.bidding.common.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private String auctionId; // Khóa ngoại liên kết tới phiên đấu giá
    private String bidderUsername; 
    private double bidAmount;

    public BidTransaction(String auctionId, String bidderUsername, double bidAmount) {
        super(); // Lấy sẵn id và thời gian đặt giá (createdAt) từ Entity
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
    }

    public String getAuctionId() { return auctionId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getBidAmount() { return bidAmount; }
}