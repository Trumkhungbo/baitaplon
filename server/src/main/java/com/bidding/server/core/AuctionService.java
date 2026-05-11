package com.bidding.server.core;

import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.AuctionNotFoundException;
import com.bidding.server.exception.InvalidBidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {

    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final AtomicInteger nextAuctionId;

    public AuctionService() {
        this.nextAuctionId = new AtomicInteger(1);
        seedData();
    }

    private void seedData() {
        addInitialAuction("seller1", "iPhone 15", 15000000, AuctionStatus.OPEN);
        addInitialAuction("seller2", "MacBook Pro", 25000000, AuctionStatus.OPEN);
        addInitialAuction("seller3", "Oil Painting", 5000000, AuctionStatus.OPEN);
    }

    private void addInitialAuction(String sellerUsername, String itemName, double startPrice, AuctionStatus status) {
        String id = String.valueOf(nextAuctionId.getAndIncrement());
        auctions.put(id, new Auction(id, sellerUsername, itemName, startPrice, status));
    }

    public String closeAuction(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {
            if (!isActiveAuction(auction)) {
                return "ERROR|Auction is not open";
            }

            return finishAuction(auction, "CLOSE_AUCTION_SUCCESS");
        }
    }

    public String getWinner(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return "WINNER_INFO|auctionId=" + auctionId
                + "|winner=" + winner
                + "|finalPrice=" + (long) auction.getCurrentPrice()
                + "|status=" + auction.getStatus();
    }

    public String getAuctionList() {
        StringBuilder sb = new StringBuilder("AUCTION_LIST|");
        boolean first = true;

        for (Auction auction : auctions.values()) {
            if (!first) {
                sb.append(";");
            }

            sb.append(auction.getId())
                    .append(":")
                    .append(auction.getItemName())
                    .append(":")
                    .append((long) auction.getCurrentPrice())
                    .append(":")
                    .append(auction.getStatus());

            first = false;
        }

        return sb.toString();
    }

    public Auction findAuctionById(String auctionId) {
        return auctions.get(auctionId);
    }

    public String getAuctionDetail(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        String bidder = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return "AUCTION_DETAIL|id=" + auction.getId()
                + "|seller=" + auction.getSellerUsername()
                + "|itemName=" + auction.getItemName()
                + "|startPrice=" + (long) auction.getStartPrice()
                + "|currentPrice=" + (long) auction.getCurrentPrice()
                + "|highestBidder=" + bidder
                + "|status=" + auction.getStatus()
                + "|endTime=" + auction.getEndTime()
                + "|bidCount=" + auction.getBidHistorySnapshot().size();
    }

    public String getBidHistory(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        StringBuilder sb = new StringBuilder("BID_HISTORY|auctionId=")
                .append(auctionId)
                .append("|entries=");
        boolean first = true;

        for (BidRecord bidRecord : auction.getBidHistorySnapshot()) {
            if (!first) {
                sb.append(";");
            }

            sb.append(bidRecord.getBidderUsername())
                    .append(",")
                    .append((long) bidRecord.getAmount())
                    .append(",")
                    .append(bidRecord.getTimestamp());
            first = false;
        }

        return sb.toString();
    }

    public String addAuction(String sellerUsername, String itemName, double startPrice) {
        if (sellerUsername == null || sellerUsername.trim().isEmpty()) {
            return "ERROR|Seller username is required";
        }

        if (itemName == null || itemName.trim().isEmpty()) {
            return "ERROR|Item name is required";
        }

        if (startPrice <= 0) {
            return "ERROR|Start price must be greater than 0";
        }

        String id = String.valueOf(nextAuctionId.getAndIncrement());
        Auction auction = new Auction(id, sellerUsername, itemName, startPrice, AuctionStatus.OPEN);
        auctions.put(id, auction);

        return "ADD_AUCTION_SUCCESS|id=" + id
                + "|seller=" + sellerUsername
                + "|itemName=" + itemName
                + "|startPrice=" + (long) startPrice;
    }
    public List<String> closeExpiredAuctions() {
        List<String> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Auction auction : auctions.values()) {
            synchronized (auction) {
                if (isActiveAuction(auction)
                        && now >= auction.getEndTime()) {
                    notifications.add(finishAuction(auction, "AUCTION_CLOSED"));
                }
            }
        }

        return notifications;
    }

    public String placeBid(String auctionId, String username, double amount) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            throw new AuctionNotFoundException("Auction not found");
        }

        synchronized (auction) {
            long now = System.currentTimeMillis();

            if (isActiveAuction(auction) && now >= auction.getEndTime()) {
                finishAuction(auction, "AUCTION_CLOSED");
                throw new AuctionClosedException("Auction is not available");
            }

            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.PAID
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                throw new AuctionClosedException("Auction is not available");
            }

            if (auction.getStatus() == AuctionStatus.OPEN) {
                auction.setStatus(AuctionStatus.RUNNING);
            }

            if (amount <= auction.getCurrentPrice()) {
                throw new InvalidBidException(
                        "Bid amount must be greater than current price ("
                                + (long) auction.getCurrentPrice() + ")"
                );
            }

            auction.setCurrentPrice(amount);
            auction.setHighestBidder(username);
            auction.addBidRecord(new BidRecord(username, amount, now));

            long remaining = auction.getEndTime() - now;
            long oldEndTime = auction.getEndTime();

            if (remaining > 0 && remaining < 30000) {
                auction.extendEndTime(60000);
                System.out.println("[ANTI-SNIPING] Auction " + auctionId
                        + " extended from " + oldEndTime
                        + " to " + auction.getEndTime());
            }

            return "BID_SUCCESS|auctionId=" + auctionId
                    + "|user=" + username
                    + "|amount=" + (long) amount;
        }
    }

    private boolean isActiveAuction(Auction auction) {
        return auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;
    }

    private String finishAuction(Auction auction, String messageType) {
        auction.setStatus(AuctionStatus.FINISHED);

        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return messageType + "|auctionId=" + auction.getId()
                + "|winner=" + winner
                + "|finalPrice=" + (long) auction.getCurrentPrice();
    }
}
