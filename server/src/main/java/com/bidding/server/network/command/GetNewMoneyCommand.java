package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class GetNewMoneyCommand implements CommandHandler {

    private final AuthService authService;

    public GetNewMoneyCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        String username = client.isLoggedIn() ? client.getCurrentUser() : (parts.length > 1 ? parts[1] : "");
        String money = parts.length > 2 ? parts[2] : "";
        if (username == null || username.isBlank() || money.isBlank()) {
            client.sendMessage("{\"command\":\"ERROR\", \"message\":\"Missing add money params\"}");
            return;
        }

        client.sendMessage(authService.addMoney(username, money));
    }
}
