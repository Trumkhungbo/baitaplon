package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;

public class Seller extends User {
    private double rating;

    public Seller() {
        super();
        setRole(UserRole.SELLER);
        this.rating = 5.0;
    }

    public Seller(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
        setRole(UserRole.SELLER);
        this.rating = 5.0;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
