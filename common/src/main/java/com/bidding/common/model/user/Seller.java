package com.bidding.common.model.user;

public class Seller extends User {
    private double rating; // Đánh giá uy tín của người bán

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.rating = 5.0; // Mặc định 5 sao
    }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    @Override
    public String getRole() {
        return "SELLER";
    }
}