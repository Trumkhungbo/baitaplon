package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;

public class GetAutoBidCommand implements CommandHandler {

    private final AuctionService auctionService;

    public GetAutoBidCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        if (parts.length < 2 || parts[1].isBlank()) {
            client.sendMessage("ERROR|Invalid syntax. Use: GET_AUTO_BID|auctionId");
            return;
        }

        client.sendMessage(auctionService.getAutoBid(parts[1], client.getCurrentUser()));
    }
}
