package com.bidding.common.model;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.item.Item;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phiên đấu giá.
 * placeBid() dùng synchronized để đảm bảo thread-safe khi nhiều client gửi cùng lúc.
 */
public class Auction {

    private long id;
    private Item item;
    private String sellerUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private double currentHighestBid;
    private String highestBidderUsername;
    private List<BidTransaction> bidHistory;

    public Auction() {
        this.bidHistory = new ArrayList<>();
    }

    public Auction(Item item, String sellerUsername,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.item = item;
        this.sellerUsername = sellerUsername;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.currentHighestBid = item.getStartingPrice();
        this.bidHistory = new ArrayList<>();
    }

    // ---- Getters & Setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public String getHighestBidderUsername() { return highestBidderUsername; }
    public void setHighestBidderUsername(String highestBidderUsername) { this.highestBidderUsername = highestBidderUsername; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<BidTransaction> bidHistory) { this.bidHistory = bidHistory; }

    /**
     * Đặt giá mới.
     * @return true nếu bid hợp lệ và được chấp nhận
     */
    public synchronized boolean placeBid(BidTransaction newBid) {
        if (this.status != AuctionStatus.RUNNING) return false;
        if (newBid.getBidAmount() <= this.currentHighestBid) return false;

        this.currentHighestBid = newBid.getBidAmount();
        this.highestBidderUsername = newBid.getBidderUsername();
        this.bidHistory.add(newBid);
        return true;
    }

    @Override
    public String toString() {
        return String.format("Auction{id=%d, item=%s, status=%s, highestBid=%.2f}",
                id, item != null ? item.getName() : "null", status, currentHighestBid);
    }
}