package com.bidding.server.model;

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
    private List<BidTransaction> bidHistory;

    public Auction() {
        super();
        this.bidHistory = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.currentHighestBid = 0;
    }

    public Auction(Item item, String sellerUsername, LocalDateTime startTime, LocalDateTime endTime) {
        this();
        this.item = item;
        this.sellerUsername = sellerUsername;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.currentHighestBid = item.getStartingPrice();
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public String getHighestBidderUsername() {
        return highestBidderUsername;
    }

    public void setHighestBidderUsername(String highestBidderUsername) {
        this.highestBidderUsername = highestBidderUsername;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<BidTransaction> bidHistory) {
        this.bidHistory = bidHistory;
    }

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
