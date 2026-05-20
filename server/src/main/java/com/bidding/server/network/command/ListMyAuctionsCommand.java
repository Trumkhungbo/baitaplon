package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;

public class ListMyAuctionsCommand implements CommandHandler {

    private final AuctionService auctionService;

    public ListMyAuctionsCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        String sellerUsername = parts.length > 1 ? parts[1] : client.getCurrentUser();
        if (!client.getCurrentUser().equals(sellerUsername) && !client.isAdmin()) {
            client.sendMessage("ERROR|You can only view your own products");
            return;
        }

        client.sendMessage(auctionService.getSellerAuctionList(sellerUsername));
    }
}
