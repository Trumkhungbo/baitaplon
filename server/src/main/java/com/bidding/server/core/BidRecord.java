package com.bidding.server.core;

public class BidRecord {

    private final String bidderUsername;
    private final double amount;
    private final long timestamp;

    public BidRecord(String bidderUsername, double amount, long timestamp) {
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
