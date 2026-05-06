package com.bidding.common.payload;

import com.bidding.common.enums.UserRole;

public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private UserRole role; // BIDDER hoặc SELLER
    private double initialBalance; // chỉ dùng khi role = BIDDER

    // Nhớ thêm Getters và Setters ở đây
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public double getInitialBalance() { return initialBalance; }
    public void setInitialBalance(double initialBalance) { this.initialBalance = initialBalance; }
}
//yêu cầu đăng kí tk mới