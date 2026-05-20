package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class DeleteAuctionCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public DeleteAuctionCommand(AuctionService auctionService, BroadcastService broadcastService) {
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
            client.sendMessage("ERROR|Invalid syntax. Use: DELETE_AUCTION|auctionId");
            return;
        }

        String response = auctionService.deleteSellerAuction(client.getCurrentUser(), parts[1]);
        client.sendMessage(response);

        if (response.startsWith("DELETE_AUCTION_SUCCESS")) {
            broadcastService.broadcastLobbyUpdate();
        }
    }
}
