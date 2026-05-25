package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;

public class ListAccountAuctionsCommand implements CommandHandler {

    private final AuctionService auctionService;

    public ListAccountAuctionsCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        String username = parts.length > 1 && !parts[1].isBlank() ? parts[1] : client.getCurrentUser();
        if (!client.getCurrentUser().equals(username) && !client.isAdmin()) {
            client.sendMessage("ERROR|You can only view your own account auctions");
            return;
        }

        client.sendMessage(auctionService.getAccountAuctionList(username));
    }
}
