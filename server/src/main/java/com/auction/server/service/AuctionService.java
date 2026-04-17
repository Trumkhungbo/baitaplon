package com.auction.server.service;

import com.auction.server.model.Auction;

import java.util.LinkedHashMap;
import java.util.Map;

public class AuctionService {

    private final Map<String, Auction> auctions;
    private int nextAuctionId;

    public AuctionService() {
        this.auctions = new LinkedHashMap<>();
        this.nextAuctionId = 1;
        seedData();
    }

    private void seedData() {
        addInitialAuction("seller1", "iPhone 15", 15000000, "OPEN");
        addInitialAuction("seller2", "MacBook Pro", 25000000, "OPEN");
        addInitialAuction("seller3", "Oil Painting", 5000000, "OPEN");
    }

    private void addInitialAuction(String sellerUsername, String itemName, double startPrice, String status) {
        String id = String.valueOf(nextAuctionId++);
        auctions.put(id, new Auction(id, sellerUsername, itemName, startPrice, status));
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
                + "|status=" + auction.getStatus();
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

        String id = String.valueOf(nextAuctionId++);
        Auction auction = new Auction(id, sellerUsername, itemName, startPrice, "OPEN");
        auctions.put(id, auction);

        return "ADD_AUCTION_SUCCESS|id=" + id
                + "|seller=" + sellerUsername
                + "|itemName=" + itemName
                + "|startPrice=" + (long) startPrice;
    }

    public String placeBid(String auctionId, String username, double amount) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {
            if (!"OPEN".equalsIgnoreCase(auction.getStatus())) {
                return "ERROR|Auction is not open";
            }

            if (amount <= auction.getCurrentPrice()) {
                return "ERROR|Bid amount must be greater than current price (" + (long) auction.getCurrentPrice() + ")";
            }

            auction.setCurrentPrice(amount);
            auction.setHighestBidder(username);

            return "BID_SUCCESS|auctionId=" + auctionId
                    + "|user=" + username
                    + "|amount=" + (long) amount;
        }
    }
}