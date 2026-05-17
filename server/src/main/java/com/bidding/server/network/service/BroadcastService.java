package com.bidding.server.network.service;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.AuctionServer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

        JsonObject response = new JsonObject();
        response.addProperty("command", "BID_UPDATE");
        response.addProperty("auctionId", auctionId);
        response.addProperty("highestBid", (long) auction.getCurrentPrice());
        response.addProperty("bidder", auction.getHighestBidder() != null ? auction.getHighestBidder() : "NONE");
        response.addProperty("startDate", auction.getStartDate());
        response.addProperty("startTime", auction.getStartClockTime());
        response.addProperty("duration", auction.getDurationMinutes());

        server.broadcastToAuctionRoom(response.toString(), auctionId);
    }

    public void broadcastAuctionClosed(String auctionId) {
        var auction = auctionService.findAuctionById(auctionId);
        if (auction == null) {
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("command", "AUCTION_CLOSED");
        response.addProperty("auctionId", auctionId);
        response.addProperty("winner", auction.getHighestBidder() != null ? auction.getHighestBidder() : "NONE");
        response.addProperty("finalPrice", (long) auction.getCurrentPrice());

        server.broadcastToAuctionRoom(response.toString(), auctionId);
    }

    public void broadcastAuctionClosedMessage(String message) {
        String auctionId = extractAuctionId(message);
        server.broadcastToAuctionRoom(message, auctionId);
    }

    public void broadcastLobbyUpdate(String message) {
        server.broadcastToLobby(message);
    }

    public void broadcastLobbyUpdate() {
        broadcastLobbyUpdate(auctionService.getAuctionList(false));
    }

    private String extractAuctionId(String message) {
        // Cố gắng đọc định dạng JSON trước
        try {
            if (message.trim().startsWith("{")) {
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                if (json.has("auctionId")) {
                    return json.get("auctionId").getAsString();
                }
            }
        } catch (Exception e) {}

        // Tương thích ngược (Fallback)
        String[] parts = message.split("\\|");
        for (String part : parts) {
            if (part.startsWith("auctionId=")) {
                return part.substring("auctionId=".length());
            }
        }

        return null;
    }
}