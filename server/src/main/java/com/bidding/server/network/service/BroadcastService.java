package com.bidding.server.network.service;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.AuctionServer;

public class BroadcastService {

    private final AuctionServer server;
    private final AuctionService auctionService;

    public BroadcastService(AuctionServer server, AuctionService auctionService) {
        this.server = server;
        this.auctionService = auctionService;
    }

    public void broadcastBidUpdate(String auctionId) {
        var auction = auctionService.findAuctionById(auctionId);
        if (auction == null) {
            return;
        }

        server.broadcastToAuctionRoom(
                "BID_UPDATE|auctionId=" + auctionId
                        + "|highestBid=" + (long) auction.getCurrentPrice()
                        + "|bidder=" + auction.getHighestBidder()
                        + "|startDate=" + auction.getStartDate()
                        + "|startTime=" + auction.getStartClockTime()
                        + "|duration=" + auction.getDurationMinutes(),
                auctionId
        );
    }

    public void broadcastAuctionClosed(String auctionId) {
        var auction = auctionService.findAuctionById(auctionId);
        if (auction == null) {
            return;
        }

        server.broadcastToAuctionRoom(
                "AUCTION_CLOSED|auctionId=" + auctionId
                        + "|winner=" + auction.getHighestBidder()
                        + "|finalPrice=" + (long) auction.getCurrentPrice(),
                auctionId
        );
    }

    public void broadcastAuctionClosedMessage(String message) {
        String auctionId = extractAuctionId(message);
        server.broadcastToAuctionRoom(message, auctionId);
    }

    public void broadcastLobbyUpdate(String message) {
        server.broadcastToLobby(message);
    }

    public void broadcastLobbyUpdate() {
        broadcastLobbyUpdate(auctionService.getAuctionList());
    }

    private String extractAuctionId(String message) {
        String[] parts = message.split("\\|");

        for (String part : parts) {
            if (part.startsWith("auctionId=")) {
                return part.substring("auctionId=".length());
            }
        }

        return null;
    }
}
