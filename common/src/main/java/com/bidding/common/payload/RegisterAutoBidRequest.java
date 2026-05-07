package com.bidding.common.payload;

public class RegisterAutoBidRequest {
    private long auctionId;
    private double maxBid;
    private double increment;

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public double getMaxBid() { return maxBid; }
    public void setMaxBid(double maxBid) { this.maxBid = maxBid; }

    public double getIncrement() { return increment; }
    public void setIncrement(double increment) { this.increment = increment; }
}
// đăng kí đấu giá tự động từ cliet lên servẻ