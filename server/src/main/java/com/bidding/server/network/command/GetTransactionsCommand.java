package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class GetTransactionsCommand implements CommandHandler {

    private final AuthService authService;

    public GetTransactionsCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        String username = parts.length > 1 && !parts[1].isBlank() ? parts[1] : client.getCurrentUser();
        if (!client.getCurrentUser().equals(username) && !client.isAdmin()) {
            client.sendMessage("ERROR|You can only view your own transactions");
            return;
        }

        client.sendMessage(authService.getTransactions(username));
    }
}
