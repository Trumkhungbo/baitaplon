package com.bidding.server.core;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private final Map<String, List<String>> userDatabase = new ConcurrentHashMap<>();
    public AuthService() {
        // Tài khoản User mặc định
        userDatabase.put("admin", List.of("123","0987654321","admin@gmail.com","123123123123"));
    }

    // Hàm đăng nhập
    public String login(String username, String password) {
        if (username == null || password == null) {
            return "LOGIN_FAILED|Back to refill username or password";
        }
        if (userDatabase.containsKey(username) && userDatabase.get(username).get(0).equals(password)) {
            return "LOGIN_SUCCESS|USER|Welcome " + username;
        }
        return "LOGIN_FAILED|Invalid username or password";
    }

    // Hàm đăng kí
    public String register(String username, List<String> infomation) {
        if (username == null || username.trim().isEmpty()) return "REGISTER_FAILED|Empty username";
        if (userDatabase.containsKey(username)) return "REGISTER_FAILED|Unable to add Username";

        userDatabase.put(username,infomation);
        return "REGISTER_SUCCESS|Register Success";
    }
    public String accoutInformation(String username){
        return "ACCOUT_SUCCESS|Account_Information|"+username+"|"+
                userDatabase.get(username).get(0)+"|"+
                userDatabase.get(username).get(1)+"|"+
                userDatabase.get(username).get(2)+"|"+
                userDatabase.get(username).get(3);
    }
}