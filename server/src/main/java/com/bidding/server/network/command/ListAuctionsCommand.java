package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;

public class ListAuctionsCommand implements CommandHandler {

    private final AuctionService auctionService;

    public ListAuctionsCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        client.setWatchingAuctionId(null);
        client.sendMessage(auctionService.getAuctionList(client.isAdmin()));
    }
}
