package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class RegisterCommand implements CommandHandler {

    private final AuthService authService;

    public RegisterCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (parts.length < 3) {
            client.sendMessage("ERROR|Invalid syntax. Use: REGISTER|username|password");
            return;
        }

        client.sendMessage(authService.register(parts[1], parts[2]));
    }
}
