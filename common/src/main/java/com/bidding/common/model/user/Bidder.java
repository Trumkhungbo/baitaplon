package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;

public class Bidder extends User {
    private double balance;

    public Bidder() {
        super();
        setRole(UserRole.BIDDER);
    }

    public Bidder(String username, String passwordHash, String email, double balance) {
        super(username, passwordHash, email);
        setRole(UserRole.BIDDER);
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
