package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class AddAuctionCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public AddAuctionCommand(AuctionService auctionService, BroadcastService broadcastService) {
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
            client.sendMessage("ERROR|Invalid syntax. Use: ADD_AUCTION|sellerUsername|itemName|startPrice");
            return;
        }

        String sellerUsername = parts[1];
        if (!client.getCurrentUser().equals(sellerUsername)) {
            client.sendMessage("ERROR|You can only create auctions for your own account");
            return;
        }
        String itemType = parts[3];
        String itemInfo = parts[4];
        String itemInfo2 = parts[5];
        double startPrice;
        try {
            startPrice = Double.parseDouble(parts[6]);
        } catch (NumberFormatException e) {
            client.sendMessage("ERROR|Start price must be a number");
            return;
        }

        client.sendMessage(auctionService.addAuction(sellerUsername, parts[2],itemType,itemInfo,itemInfo2, startPrice));
        broadcastService.broadcastLobbyUpdate();
    }
}
