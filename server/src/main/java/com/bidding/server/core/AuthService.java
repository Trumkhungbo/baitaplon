package com.bidding.server.core;

import com.bidding.common.enums.UserRole;
import com.bidding.common.model.user.Bidder;
import com.bidding.common.model.user.User;
import com.bidding.server.database.DatabaseInitializer;
import com.bidding.server.repository.TopUpRequestDAO;
import com.bidding.server.repository.UserDAO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Locale;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final TopUpRequestDAO topUpRequestDAO = new TopUpRequestDAO();

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
        double balanceValue = userDAO.getBalanceByUsername(username);
        response.addProperty("balance", String.format(Locale.US, "%.2f", balanceValue));
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

            if (amountToAdd <= 0) {
                response.addProperty("command", "ERROR");
                response.addProperty("message", "Amount must be positive");
                return response.toString();
            }

            long requestId = topUpRequestDAO.create(cleanUsername, amountToAdd);
            response.addProperty("command", "TOPUP_REQUEST_CREATED");
            response.addProperty("requestId", requestId);
            response.addProperty("message", "Yeu cau nap tien dang cho admin duyet");
            return response.toString();

        } catch (NumberFormatException e) {
            response.addProperty("command", "ERROR");
            response.addProperty("message", "Invalid money format");
            return response.toString();
        }
    }

    public String listUsers() {
        JsonObject response = new JsonObject();
        response.addProperty("command", "ADMIN_USERS");
        JsonArray users = new JsonArray();
        for (User user : userDAO.findAll()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", user.getId());
            item.addProperty("username", safe(user.getUsername()));
            item.addProperty("email", safe(user.getEmail()));
            item.addProperty("phone", safe(user.getPhone()));
            item.addProperty("personalID", safe(user.getPersonalId()));
            item.addProperty("role", user.getRole() == null ? "" : user.getRole().name());
            item.addProperty("balance", String.format(Locale.US, "%.2f", userDAO.getBalanceByUsername(user.getUsername())));
            item.addProperty("createdAt", user.getCreatedAt());
            users.add(item);
        }
        response.add("users", users);
        return response.toString();
    }

    public String deleteUser(String userId) {
        JsonObject response = new JsonObject();
        response.addProperty("command", "ADMIN_USER_DELETE_RESULT");
        try {
            long id = Long.parseLong(userId);
            User user = userDAO.findById(id);
            if (user == null) {
                response.addProperty("status", "FAILED");
                response.addProperty("message", "User not found");
                return response.toString();
            }
            if (user.getRole() == UserRole.ADMIN) {
                response.addProperty("status", "FAILED");
                response.addProperty("message", "Cannot delete admin account");
                return response.toString();
            }
            boolean deleted = userDAO.delete(id);
            response.addProperty("status", deleted ? "SUCCESS" : "FAILED");
            response.addProperty("message", deleted ? "Deleted user" : "Unable to delete user");
            return response.toString();
        } catch (Exception e) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Unable to delete user: " + e.getMessage());
            return response.toString();
        }
    }

    public String listTopUpRequests() {
        JsonObject response = new JsonObject();
        response.addProperty("command", "ADMIN_TOPUP_REQUESTS");
        JsonArray requests = new JsonArray();
        for (TopUpRequestDAO.TopUpRequest request : topUpRequestDAO.findPending()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", request.id());
            item.addProperty("username", safe(request.username()));
            item.addProperty("currentBalance", String.format(Locale.US, "%.2f", request.currentBalance()));
            item.addProperty("amount", String.format(Locale.US, "%.2f", request.amount()));
            item.addProperty("email", safe(request.email()));
            item.addProperty("phone", safe(request.phone()));
            item.addProperty("personalID", safe(request.personalId()));
            item.addProperty("requestedAt", request.requestedAt());
            requests.add(item);
        }
        response.add("requests", requests);
        return response.toString();
    }

    public String approveTopUpRequest(String requestId) {
        JsonObject response = new JsonObject();
        response.addProperty("command", "ADMIN_TOPUP_APPROVE_RESULT");
        try {
            long id = Long.parseLong(requestId);
            TopUpRequestDAO.TopUpRequest request = topUpRequestDAO.findPendingById(id);
            if (request == null) {
                response.addProperty("status", "FAILED");
                response.addProperty("message", "Top-up request not found");
                return response.toString();
            }
            double newBalance = request.currentBalance() + request.amount();
            userDAO.updateBalance(request.username(), newBalance);
            topUpRequestDAO.markApproved(id);
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Approved top-up request");
            return response.toString();
        } catch (Exception e) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Unable to approve top-up: " + e.getMessage());
            return response.toString();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
