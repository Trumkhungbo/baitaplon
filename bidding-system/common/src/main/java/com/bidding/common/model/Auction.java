package com.bidding.common.model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.item.Item;

public class Auction extends Entity {
    private Item item;
    private String sellerUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private double currentHighestBid;
    private String highestBidderUsername;
    
    // Lưu lịch sử các lần đặt giá
    private List<BidTransaction> bidHistory;

    public Auction(Item item, String sellerUsername, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.item = item;
        this.sellerUsername = sellerUsername;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.currentHighestBid = item.getStartingPrice();
        this.bidHistory = new ArrayList<>();
    }

    // Getters
    public Item getItem() { return item; }
    public AuctionStatus getStatus() { return status; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }

    public void setStatus(AuctionStatus status) { this.status = status; }

    // Logic thêm bid mới
    public synchronized boolean placeBid(BidTransaction newBid) {
        if (this.status != AuctionStatus.RUNNING) {
            return false;
        }
        if (newBid.getBidAmount() > this.currentHighestBid) {
            this.currentHighestBid = newBid.getBidAmount();
            this.highestBidderUsername = newBid.getBidderUsername();
            this.bidHistory.add(newBid);
            return true;
        }
        return false;
    }
}