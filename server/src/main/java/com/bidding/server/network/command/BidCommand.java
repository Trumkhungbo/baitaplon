package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class BidCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public BidCommand(AuctionService auctionService, BroadcastService broadcastService) {
        this.auctionService = auctionService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        if (parts.length < 3) {
            client.sendMessage("ERROR|Invalid syntax. Use: BID|auctionId|amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            client.sendMessage("ERROR|Bid amount must be a number");
            return;
        }

        if (amount <= 0) {
            client.sendMessage("ERROR|Bid amount must be greater than 0");
            return;
        }

        try {
            String response = auctionService.placeBid(parts[1], client.getCurrentUser(), amount);
            client.sendMessage(response);

            if (response.startsWith("BID_RESULT|status=SUCCESS")
                    || response.startsWith("BID_SUCCESS")) {
                broadcastService.broadcastBidUpdate(parts[1]);
                broadcastService.broadcastLobbyUpdate();
            }
        } catch (RuntimeException e) {
            client.sendMessage("ERROR|" + e.getMessage());
        }
    }
}
