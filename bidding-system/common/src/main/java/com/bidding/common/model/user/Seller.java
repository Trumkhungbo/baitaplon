package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;

public class Seller extends User {

    private double rating; // Đánh giá uy tín 0–5

    public Seller() {}

    public Seller(String username, String passwordHash, String email, String fullName) {
        super(username, passwordHash, email, fullName, UserRole.SELLER);
        this.rating = 5.0;
    }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    @Override
    public String getDescription() {
        return String.format("Người bán: %s | Đánh giá: %.1f⭐", getUsername(), rating);
    }
}