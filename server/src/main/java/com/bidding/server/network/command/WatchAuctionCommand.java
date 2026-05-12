package com.bidding.server.network.command;

import com.bidding.server.network.ClientHandler;

public class WatchAuctionCommand implements CommandHandler {

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (parts.length < 2) {
            client.sendMessage("ERROR|Invalid syntax. Use: WATCH|auctionId");
            return;
        }

        client.setWatchingAuctionId(parts[1]);
        client.sendMessage("WATCHING|You are now watching auction " + client.getWatchingAuctionId());
    }
}
