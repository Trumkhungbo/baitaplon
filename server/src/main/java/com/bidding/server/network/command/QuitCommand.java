package com.bidding.server.network.command;

import com.bidding.server.network.ClientHandler;

public class QuitCommand implements CommandHandler {

    @Override
    public void handle(String[] parts, ClientHandler client) {
        client.sendMessage("BYE|Disconnected from server");
        client.disconnect();
    }
}
