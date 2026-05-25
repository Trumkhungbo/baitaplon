package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class PayAuctionCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public PayAuctionCommand(AuctionService auctionService, BroadcastService broadcastService) {
        this.auctionService = auctionService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        if (parts.length < 2 || parts[1].isBlank()) {
            client.sendMessage("PAY_AUCTION_RESULT|status=FAILED|message=Auction id required");
            return;
        }

        String response = auctionService.payAuction(parts[1], client.getCurrentUser());
        client.sendMessage(response);

        if (response.startsWith("PAY_AUCTION_RESULT|status=SUCCESS")
                || response.contains("|newStatus=CANCELED|")) {
            broadcastService.broadcastLobbyUpdate();
            broadcastService.broadcastAuctionClosedMessage(response.replace("PAY_AUCTION_RESULT", "AUCTION_PAYMENT_UPDATE"));
        }
    }
}
