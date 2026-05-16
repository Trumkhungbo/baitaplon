package com.bidding.server.network.command;

import com.bidding.common.enums.UserRole;
import com.bidding.server.network.ClientHandler;

public class ElevateCommand implements CommandHandler {

    @Override
    public void handle(String[] parts, ClientHandler client) {
        client.setCurrentRole(UserRole.ADMIN);
        client.sendMessage("ELEVATE_SUCCESS|You are now an admin.");
    }
}