package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class GetAccountInformationCommand implements CommandHandler {

    private final AuthService authService;

    public GetAccountInformationCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (parts.length < 2) {
            client.sendMessage("ERROR|Invalid syntax. Use: GET_ACCOUNTINFORMATION|username");
            return;
        }

        client.sendMessage(authService.accountInformation(parts[1]));
    }
}
