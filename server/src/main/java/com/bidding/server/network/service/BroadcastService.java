package com.bidding.server.network.service;
import com.bidding.server.network.AuctionServer;
import com.bidding.server.core.AuctionService;

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

        // Khắc phục lỗi in chữ "null" ra màn hình người dùng
        String bidder = auction.getHighestBidder() != null ? auction.getHighestBidder() : "NONE";

        server.broadcastToAuctionRoom(
                "BID_UPDATE|auctionId=" + auctionId
                        + "|highestBid=" + (long) auction.getCurrentPrice()
                        + "|bidder=" + bidder
                        + "|duration=" + auction.getDurationMinutes()
                        + "|endTime=" + auction.getEndTime(),
                auctionId
        );
    }

    public void broadcastAuctionClosed(String auctionId) {
        var auction = auctionService.findAuctionById(auctionId);
        if (auction == null) {
            return;
        }

        // Khắc phục lỗi in chữ "null" khi cuộc đấu giá không có người mua
        String winner = auction.getHighestBidder() != null ? auction.getHighestBidder() : "NONE";

        server.broadcastToAuctionRoom(
                "AUCTION_CLOSED|auctionId=" + auctionId
                        + "|winner=" + winner
                        + "|finalPrice=" + (long) auction.getCurrentPrice(),
                auctionId
        );
    }

    public void broadcastAuctionClosedMessage(String message) {
        String auctionId = extractAuctionId(message);
        // Kiểm tra an toàn để bảo vệ Server không bị crash do NullPointerException
        if (auctionId != null) {
            server.broadcastToAuctionRoom(message, auctionId);
        }
    }

    public void broadcastLobbyUpdate(String message) {
        server.broadcastToLobby(message);
    }

    public void broadcastLobbyUpdate() {
        broadcastLobbyUpdate(auctionService.getAuctionList(false));
    }

    private String extractAuctionId(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }

        String[] parts = message.split("\\|");
        for (String part : parts) {
            if (part.startsWith("auctionId=")) {
                return part.substring("auctionId=".length());
            }
        }

        return null;
    }
}
