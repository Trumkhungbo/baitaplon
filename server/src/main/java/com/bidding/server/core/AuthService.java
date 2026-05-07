package com.bidding.server.core;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final Map<String, String> userDatabase = new ConcurrentHashMap<>();
    public AuthService() {
        // Tài khoản Admin mặc định
        userDatabase.put("admin", "123");
    }

    // Hàm đăng nhập
    public String login(String username, String password) {
        if (username == null || password == null) {
            return "LOGIN_FAILED|Back to refill username or password";
        }
        if (userDatabase.containsKey(username) && userDatabase.get(username).equals(password)) {
            return "LOGIN_SUCCESS|USER|Welcome " + username;
        }
        return "LOGIN_FAILED|Invalid username or password";
    }

    // Hàm đăng kí
    public String register(String username, String password) {
        if (username == null || username.trim().isEmpty()) return "REGISTER_FAILED|Empty username";
        if (userDatabase.containsKey(username)) return "REGISTER_FAILED|Unable to add Username";

        userDatabase.put(username, password);
        return "REGISTER_SUCCESS|Register Success";
    }
}