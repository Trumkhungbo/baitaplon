package com.bidding.common.payload;

public class AuctionIdRequest {
    private long auctionId;

    // Nhớ thêm Getters và Setters ở đây
    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }
}
// gửi yêu cầu từ client lên server để lấy thông tin đấu giá hoặc làm các tác vụ 
// liên quan đến daus giá