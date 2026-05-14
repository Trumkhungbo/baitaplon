package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class SetAutoBidCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public SetAutoBidCommand(AuctionService auctionService, BroadcastService broadcastService) {
        this.auctionService = auctionService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        if (parts.length < 4) {
            client.sendMessage("ERROR|Invalid syntax. Use: SET_AUTO_BID|auctionId|maxBid|increment");
            return;
        }

        double maxBid;
        double increment;
        try {
            maxBid = Double.parseDouble(parts[2]);
            increment = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            client.sendMessage("ERROR|Auto-bid values must be numbers");
            return;
        }

        if (maxBid <= 0 || increment <= 0) {
            client.sendMessage("ERROR|Max bid and increment must be greater than 0");
            return;
        }

        try {
            String response = auctionService.setAutoBid(parts[1], client.getCurrentUser(), maxBid, increment);
            client.sendMessage(response);

            if (response.startsWith("AUTO_BID_SET")) {
                broadcastService.broadcastBidUpdate(parts[1]);
                broadcastService.broadcastLobbyUpdate();
            }
        } catch (RuntimeException e) {
            client.sendMessage("ERROR|" + e.getMessage());
        }
    }
}
