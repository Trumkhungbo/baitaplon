package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

import java.util.ArrayList;
import java.util.Arrays;

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
        String password = parts[2];
        String phoneNumber = parts[3];
        String email = parts[4];
        String personalID = parts[5];
        ArrayList<String> AccoutInformation = new ArrayList<>(Arrays.asList(password, phoneNumber, email, personalID,"0"));
        client.sendMessage(authService.register(parts[1], AccoutInformation));
    }
}
