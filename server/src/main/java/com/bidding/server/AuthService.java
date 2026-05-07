package com.bidding.server;

public class AuthService {

    public String login(String username, String password) {
        if (username == null || password == null) {
            return "LOGIN_FAILED|Invalid username or password";
        }

        if ("admin".equals(username) && "123".equals(password)) {
            return "LOGIN_SUCCESS|Welcome " + username;
        }

        if (!username.trim().isEmpty() && !password.trim().isEmpty()) {
            return "LOGIN_SUCCESS|Welcome " + username;
        }

        return "LOGIN_FAILED|Invalid username or password";
    }
}