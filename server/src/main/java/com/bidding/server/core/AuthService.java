package com.bidding.server.core;

import com.bidding.common.enums.UserRole;
import com.bidding.common.model.user.Bidder;
import com.bidding.common.model.user.User;
import com.bidding.server.database.DatabaseInitializer;
import com.bidding.server.repository.UserDAO;
import com.google.gson.JsonObject;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    public AuthService() {
        DatabaseInitializer.initialize();
    }

    public String login(String username, String password) {
        if (username == null || password == null) {
            return "LOGIN_FAILED|Back to refill username or password";
        }

        User foundUser = userDAO.findByUsername(username);
        if (foundUser != null && PasswordHasher.matches(password, foundUser.getPasswordHash())) {
            if (PasswordHasher.needsUpgrade(foundUser.getPasswordHash())) {
                userDAO.updatePasswordHash(username, PasswordHasher.hash(password));
            }
            return "LOGIN_SUCCESS|USER|Welcome " + username;
        }
        return "LOGIN_FAILED|Invalid username or password";
    }

    public String register(String username, String password,String phoneNumber,String email,String personalID) {
        if (username == null || username.trim().isEmpty()) {
            return "REGISTER_FAILED|Empty username";
        }
        if (userDAO.existsByUsername(username)) {
            return "REGISTER_FAILED|Unable to add Username";
        }
        String normalizedEmail = (email == null || email.trim().isEmpty())
                ? username.trim() + "@local.auction"
                : email.trim();

        Bidder newUser = new Bidder();
        newUser.setUsername(username);
        newUser.setPasswordHash(PasswordHasher.hash(password));
        newUser.setEmail(normalizedEmail);
        newUser.setPhone(phoneNumber);
        newUser.setPersonalId(personalID);
        newUser.setRole(UserRole.BIDDER);
        newUser.setBalance(0.0);
        newUser.setCreatedAt(System.currentTimeMillis());

        try {
            userDAO.save(newUser);
            return "REGISTER_SUCCESS|Register Success";
        } catch (RuntimeException e) {
            return "REGISTER_FAILED|Unable to add Username";
        }
    }

    public String accountInformation(String username) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            return "ERROR|User not found";
        }

        JsonObject response = new JsonObject();
        response.addProperty("command", "ACCOUNT_INFO");
        response.addProperty("username", user.getUsername());
        response.addProperty("password", "********");
        response.addProperty("email", user.getEmail() != null ? user.getEmail() : "");
        response.addProperty("fullName", "");
        response.addProperty("phone", user.getPhone() != null ? user.getPhone() : "");
        response.addProperty("personalID", user.getPersonalId() != null ? user.getPersonalId() : "");
        double balanceValue = (user instanceof Bidder bidder) ? bidder.getBalance() : 0.0;
        response.addProperty("balance", String.format("%.2f", balanceValue));
        return response.toString();
    }

    public String forgotPassword(String username, String phone, String personalID) {
        User user = userDAO.findByUsername(username);

        JsonObject response = new JsonObject();
        response.addProperty("command", "FORGOT_PASSWORD_RESULT");

        if (user != null
                && user.getPhone() != null
                && user.getPhone().equals(phone)
                && user.getPersonalId() != null
                && user.getPersonalId().equals(personalID)) {
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Thong tin hop le. Can reset mat khau thay vi hien mat khau.");
        } else {
            response.addProperty("status", "FAILED");
        }

        return response.toString();
    }

    public String resetPassword(String username, String newPassword) {
        JsonObject response = new JsonObject();
        response.addProperty("command", "RESET_PASSWORD_RESULT");
        try {
            User user = userDAO.findByUsername(username);
            if (user != null) {
                userDAO.updatePasswordHash(username, newPassword);

                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Đổi mật khẩu thành công!");
            } else {
                response.addProperty("status", "FAILED");
                response.addProperty("message", "Không tìm thấy tài khoản '" + username + "' trong hệ thống!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Lỗi CSDL: Không thể lưu mật khẩu mới.");
        }
        return response.toString();
    }

    public String addMoney(String username, String money) {
        try {
            String cleanUsername = username.trim();
            double amountToAdd = Double.parseDouble(money.trim());

            User user = userDAO.findByUsername(cleanUsername);
            if (user == null) {
                return "{\"command\":\"ERROR\", \"message\":\"User not found\"}";
            }

            if (user instanceof Bidder bidder) {
                double newTotal = bidder.getBalance() + amountToAdd;
                userDAO.updateBalance(cleanUsername, newTotal);

                JsonObject response = new JsonObject();
                response.addProperty("command", "MONEY_UPDATE");
                response.addProperty("balance", String.format("%.2f", newTotal));
                return response.toString();
            }

            return "{\"command\":\"ERROR\", \"message\":\"Only bidders can add money\"}";
        } catch (NumberFormatException e) {
            return "{\"command\":\"ERROR\", \"message\":\"Invalid money format\"}";
        }
    }
}