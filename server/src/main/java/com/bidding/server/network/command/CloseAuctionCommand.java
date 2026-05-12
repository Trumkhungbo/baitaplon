package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class CloseAuctionCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public CloseAuctionCommand(AuctionService auctionService, BroadcastService broadcastService) {
        this.auctionService = auctionService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        if (parts.length < 2) {
            client.sendMessage("ERROR|Invalid syntax. Use: CLOSE_AUCTION|auctionId");
            return;
        }

        String auctionId = parts[1];
        var auction = auctionService.findAuctionById(auctionId);

        if (auction == null) {
            client.sendMessage("ERROR|Auction not found");
            return;
        }

        if (!client.isAdmin() && !client.getCurrentUser().equals(auction.getSellerUsername())) {
            client.sendMessage("ERROR|Only the seller or admin can close this auction");
            return;
        }

        String response = auctionService.closeAuction(auctionId);
        client.sendMessage(response);

        if (response.startsWith("CLOSE_AUCTION_SUCCESS")) {
            broadcastService.broadcastAuctionClosed(auctionId);
            broadcastService.broadcastLobbyUpdate();
        }
    }
}
