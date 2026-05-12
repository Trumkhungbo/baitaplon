package com.bidding.server.network.command;

import com.bidding.server.network.ClientHandler;

public class PingCommand implements CommandHandler {

    @Override
    public void handle(String[] parts, ClientHandler client) {
        client.sendMessage("PONG");
    }
}
