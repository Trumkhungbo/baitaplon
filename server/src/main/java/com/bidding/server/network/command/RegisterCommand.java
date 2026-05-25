package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;
import com.google.gson.JsonObject;

public class RegisterCommand implements CommandHandler {

    private final AuthService authService;

    public RegisterCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (parts.length < 3) {
            JsonObject err = new JsonObject();
            err.addProperty("command", "REGISTER_RESULT");
            err.addProperty("status", "FAILED");
            err.addProperty("message", "Invalid syntax.");
            client.sendMessage(err.toString());
            return;
        }

        String phone = parts.length > 3 ? parts[3] : "";
        String email = parts.length > 4 ? parts[4] : "";
        String personalId = parts.length > 5 ? parts[5] : "";
        client.sendMessage(authService.register(parts[1], parts[2], phone, email, personalId));
    }
}
