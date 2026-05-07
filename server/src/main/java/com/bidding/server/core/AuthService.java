package com.bidding.server.core;

public class AuthService {

    public String login(String username, String password) {
        if (username == null || password == null) {
            return "LOGIN_FAILED|Invalid username or password";
        }

        username = username.trim();
        password = password.trim();

        if (username.isEmpty() || password.isEmpty()) {
            return "LOGIN_FAILED|Invalid username or password";
        }

        if ("admin".equals(username) && "123".equals(password)) {
            return "LOGIN_SUCCESS|Welcome " + username;
        }

        return "LOGIN_SUCCESS|Welcome " + username;
    }
}
