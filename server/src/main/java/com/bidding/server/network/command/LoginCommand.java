package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

        try {
            // Đọc JSON để xem đăng nhập có SUCCESS không, nếu có thì gán phiên làm việc
            JsonObject resObj = JsonParser.parseString(response).getAsJsonObject();
            if ("SUCCESS".equals(resObj.get("status").getAsString())) {
                client.setCurrentUser(username);
                client.setCurrentRole(authService.getUserRole(username));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        client.sendMessage(response);
    }
}
