package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;

public class GetWinnerCommand implements CommandHandler {

    private final AuctionService auctionService;

    public GetWinnerCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (parts.length < 2) {
            client.sendMessage("ERROR|Invalid syntax. Use: GET_WINNER|auctionId");
            return;
        }

        client.sendMessage(auctionService.getWinner(parts[1]));
    }
}
