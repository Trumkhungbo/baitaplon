package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class AdminDeleteUserCommand implements CommandHandler {
    private final AuthService authService;

    public AdminDeleteUserCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isAdmin()) {
            client.sendMessage("{\"command\":\"ERROR\", \"message\":\"Admin permission required\"}");
            return;
        }
        String userId = parts.length > 1 ? parts[1] : "";
        client.sendMessage(authService.deleteUser(userId));
    }
}
