package com.bidding.common.model;

public class Bidder extends User {
    private double balance; // Số dư tài khoản để tham gia đấu giá

    public Bidder(String username, String password, String email, double balance) {
        super(username, password, email);
        this.balance = balance;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public String getRole() {
        return "BIDDER";
    }
}