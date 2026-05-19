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
        JsonObject response = new JsonObject();
        response.addProperty("command", "LOGIN_RESULT");

        if (username == null || password == null) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Back to refill username or password");
            return response.toString();
        }

        User foundUser = userDAO.findByUsername(username);
        if (foundUser != null && PasswordHasher.matches(password, foundUser.getPasswordHash())) {
            if (PasswordHasher.needsUpgrade(foundUser.getPasswordHash())) {
                userDAO.updatePasswordHash(username, PasswordHasher.hash(password));
            }
            response.addProperty("status", "SUCCESS");
            response.addProperty("role", foundUser.getRole().name());
            response.addProperty("message", "Welcome " + username);
            return response.toString();
        }

        response.addProperty("status", "FAILED");
        response.addProperty("message", "Invalid username or password");
        return response.toString();
    }

    public UserRole getUserRole(String username) {
        User foundUser = userDAO.findByUsername(username);
        return foundUser != null ? foundUser.getRole() : null;
    }

    public String register(String username, String password,String phoneNumber,String email,String personalID) {
        JsonObject response = new JsonObject();
        response.addProperty("command", "REGISTER_RESULT");

        if (username == null || username.trim().isEmpty()) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Empty username");
            return response.toString();
        }
        String normalizedEmail = (email == null || email.trim().isEmpty())
                ? username.trim() + "@local.auction"
                : email.trim();
        if (userDAO.existsByUsername(username)) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Username already exists");
            return response.toString();
        }
        if (normalizedEmailExists(normalizedEmail)) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Email already exists");
            return response.toString();
        }

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
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Register Success");
        } catch (RuntimeException e) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", resolveRegisterErrorMessage(e));
        }
        return response.toString();
    }

    private boolean normalizedEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return userDAO.existsByEmail(email.trim());
    }

    private String resolveRegisterErrorMessage(RuntimeException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (message.contains("users.username")) {
            return "Username already exists";
        }
        if (message.contains("users.email")) {
            return "Email already exists";
        }
        return "Unable to create account";
    }

    public String accountInformation(String username) {
        User user = userDAO.findByUsername(username);

        if (user == null) {
            JsonObject err = new JsonObject();
            err.addProperty("command", "ERROR");
            err.addProperty("message", "User not found");
            return err.toString();
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
                userDAO.updatePasswordHash(username, PasswordHasher.hash(newPassword));

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
        JsonObject response = new JsonObject();

        try {
            String cleanUsername = username.trim();
            double amountToAdd = Double.parseDouble(money.trim());

            User user = userDAO.findByUsername(cleanUsername);
            if (user == null) {
                response.addProperty("command", "ERROR");
                response.addProperty("message", "User not found");
                return response.toString();
            }

            if (user instanceof Bidder bidder) {
                double newTotal = bidder.getBalance() + amountToAdd;
                userDAO.updateBalance(cleanUsername, newTotal);

                response.addProperty("command", "MONEY_UPDATE");
                response.addProperty("balance", String.format("%.2f", newTotal));
                return response.toString();
            }

            response.addProperty("command", "ERROR");
            response.addProperty("message", "Only bidders can add money");
            return response.toString();

        } catch (NumberFormatException e) {
            response.addProperty("command", "ERROR");
            response.addProperty("message", "Invalid money format");
            return response.toString();
        }
    }
}
