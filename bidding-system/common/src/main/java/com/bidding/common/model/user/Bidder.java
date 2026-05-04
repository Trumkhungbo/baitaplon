package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;

public class Bidder extends User {

    private double balance; // Số dư tài khoản

    public Bidder() {}

    public Bidder(String username, String passwordHash, String email,
                  String fullName, double balance) {
        super(username, passwordHash, email, fullName, UserRole.BIDDER);
        this.balance = balance;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public String getDescription() {
        return String.format("Người đặt giá: %s | Số dư: $%.2f", getUsername(), balance);
    }
}