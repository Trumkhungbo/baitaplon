package com.bidding.server.model.user;

public class Admin extends User {
    
    public Admin(String username, String password, String email) {
        super(username, password, email);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}