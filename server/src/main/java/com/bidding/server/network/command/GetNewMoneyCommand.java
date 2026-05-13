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
        if (parts.length < 3) {
            client.sendMessage("{\"command\":\"ERROR\", \"message\":\"Missing add money params\"}");
            return;
        }

        client.sendMessage(authService.addMoney(parts[1], parts[2]));
    }
}
