package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;

public class GetAuctionDetailCommand implements CommandHandler {

    private final AuctionService auctionService;

    public GetAuctionDetailCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (parts.length < 2) {
            client.sendMessage("ERROR|Invalid syntax. Use: GET_AUCTION_DETAIL|auctionId");
            return;
        }

        client.sendMessage(auctionService.getAuctionDetail(parts[1]));
    }
}
