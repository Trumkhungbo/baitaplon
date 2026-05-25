package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class AdminListUsersCommand implements CommandHandler {
    private final AuthService authService;

    public AdminListUsersCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isAdmin()) {
            client.sendMessage("{\"command\":\"ERROR\", \"message\":\"Admin permission required\"}");
            return;
        }
        client.sendMessage(authService.listUsers());
    }
}
