package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;

public class GetBidHistoryCommand implements CommandHandler {

    private final AuctionService auctionService;

    public GetBidHistoryCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (parts.length < 2) {
            client.sendMessage("ERROR|Invalid syntax. Use: GET_BID_HISTORY|auctionId");
            return;
        }

        client.sendMessage(auctionService.getBidHistory(parts[1]));
    }
}
