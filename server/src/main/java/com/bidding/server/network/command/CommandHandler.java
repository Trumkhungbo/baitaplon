package com.bidding.server.network.command;

import com.bidding.server.network.ClientHandler;

public interface CommandHandler {
    void handle(String[] parts, ClientHandler client);
}
