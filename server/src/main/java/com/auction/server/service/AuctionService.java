package com.auction.server.service;

import com.auction.server.model.Auction;

import java.util.LinkedHashMap;
import java.util.Map;

public class AuctionService {

    private final Map<String, Auction> auctions;

    public AuctionService() {
        this.auctions = new LinkedHashMap<>();
        seedData();
    }

    private void seedData() {
        auctions.put("1", new Auction("1", "iPhone 15", 15000000, "OPEN"));
        auctions.put("2", new Auction("2", "MacBook Pro", 25000000, "OPEN"));
        auctions.put("3", new Auction("3", "Oil Painting", 5000000, "OPEN"));
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
                    + "|amount=" + amount;
        }
    }
}