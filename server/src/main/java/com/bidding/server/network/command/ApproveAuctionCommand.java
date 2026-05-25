package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class ApproveAuctionCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public ApproveAuctionCommand(AuctionService auctionService, BroadcastService broadcastService) {
        this.auctionService = auctionService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isAdmin()) {
            client.sendMessage("ERROR|Only admin can approve auctions");
            return;
        }

        if (parts.length < 2) {
            client.sendMessage("ERROR|Invalid syntax. Use: APPROVE_AUCTION|auctionId");
            return;
        }

        String response = auctionService.approveAuction(parts[1]);
        client.sendMessage(response);

        if (response.startsWith("APPROVE_AUCTION_SUCCESS")) {
            broadcastService.broadcastLobbyUpdate();
        }
    }
}
