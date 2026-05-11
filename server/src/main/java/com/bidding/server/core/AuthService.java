package com.bidding.server.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final Map<String, List<String>> userDatabase = new ConcurrentHashMap<>();

    public AuthService() {
        userDatabase.put("admin", List.of("123", "0987654321", "admin@gmail.com", "123123123123"));
    }

    public String login(String username, String password) {
        if (username == null || password == null) {
            return "LOGIN_FAILED|Back to refill username or password";
        }
        if (userDatabase.containsKey(username) && userDatabase.get(username).get(0).equals(password)) {
            return "LOGIN_SUCCESS|USER|Welcome " + username;
        }
        return "LOGIN_FAILED|Invalid username or password";
    }

    public String register(String username, String password) {
        if (username == null || username.trim().isEmpty()) return "REGISTER_FAILED|Empty username";
        if (userDatabase.containsKey(username)) return "REGISTER_FAILED|Unable to add Username";

        userDatabase.put(username, List.of(password, "", "", ""));
        return "REGISTER_SUCCESS|Register Success";
    }

    public String accountInformation(String username) {
        List<String> info = userDatabase.get(username);
        if (info == null) {
            return "ACCOUNT_FAILED|User not found";
        }

        return "ACCOUT_SUCCESS|Account_Information|" + username + "|"
                + info.get(0) + "|"
                + info.get(1) + "|"
                + info.get(2) + "|"
                + info.get(3);
    }
}
