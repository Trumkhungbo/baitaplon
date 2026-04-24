package com.bidding.server.model;

public class Auction {

    public enum Status {
        ACTIVE,
        CLOSED
    }

    private long id;
    private long itemId;
    private long sellerId;

    private double startingPrice;
    private double currentPrice;

    private long startTime;
    private long endTime;

    private Status status;

    private Long leadBidderId;
    private Long winnerId;

    // ===== Getter & Setter =====
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public long getSellerId() { return sellerId; }
    public void setSellerId(long sellerId) { this.sellerId = sellerId; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Long getLeadBidderId() { return leadBidderId; }
    public void setLeadBidderId(Long leadBidderId) { this.leadBidderId = leadBidderId; }

    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }
}