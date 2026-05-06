package com.bidding.common.payload;

public class BidRequest {
    private long auctionId;
    private double bidAmount;

    // Phải viết thêm Getters và Setters cho chuẩn bảo mật
    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }
}

// Đây là payload đơn giản để client gửi yêu cầu đặt giá thầu.
// ktra bidAmount phải lớn hơn giá hiện tại của đấu giá và phải nhỏ hơn hoặc bằng số dư của người dùng.
