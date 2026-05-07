package com.bidding.common.model;

/** Cấu hình đặt giá tự động của một người dùng cho một phiên */
public class AutoBid extends Entity { // 1. Thêm kế thừa từ Entity

    // 2. Đã xóa biến 'id' vì nó đã có sẵn trong lớp cha Entity
    private long auctionId;
    private String bidderUsername;
    private double maxBid;       // Mức giá tối đa chấp nhận trả
    private double increment;    // Bước tăng mỗi lần auto-bid
    private boolean active;

    public AutoBid() {
        super(); // 3. Gọi hàm khởi tạo của Entity để gán id=0 và createdAt
    }

    public AutoBid(long auctionId, String bidderUsername, double maxBid, double increment) {
        super(); // 4. Gọi hàm khởi tạo của Entity
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.maxBid = maxBid;
        this.increment = increment;
        this.active = true;
    }

    // 5. Đã xóa các hàm getId() và setId() vì kế thừa từ Entity

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }

    public double getMaxBid() { return maxBid; }
    public void setMaxBid(double maxBid) { this.maxBid = maxBid; }

    public double getIncrement() { return increment; }
    public void setIncrement(double increment) { this.increment = increment; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}