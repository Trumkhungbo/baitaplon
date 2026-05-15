package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;
import com.bidding.common.enums.UserRole;

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
            String[] responseParts = response.split("\\|");
            if (responseParts.length > 1) {
                try {
                    client.setCurrentRole(UserRole.valueOf(responseParts[1]));
                } catch (IllegalArgumentException e) {
                    client.setCurrentRole(null);
                }
            }
        }

        client.sendMessage(response);
    }
}
