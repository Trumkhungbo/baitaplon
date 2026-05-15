package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class LoginCommand implements CommandHandler {

    private final AuthService authService;

    public LoginCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (parts.length < 3) {
            client.sendMessage("ERROR|Invalid syntax. Use: LOGIN|username|password");
            return;
        }

        String username = parts[1];
        String response = authService.login(username, parts[2]);

        if (response.startsWith("LOGIN_SUCCESS")) {
            client.setCurrentUser(username);
            client.setCurrentRole(authService.getUserRole(username));
        }

        client.sendMessage(response);
    }
}
