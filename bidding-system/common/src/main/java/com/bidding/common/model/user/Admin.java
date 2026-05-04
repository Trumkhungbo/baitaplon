package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;

public class Admin extends User {

    public Admin() {}

    public Admin(String username, String passwordHash, String email, String fullName) {
        super(username, passwordHash, email, fullName, UserRole.ADMIN);
    }

    @Override
    public String getDescription() {
        return "Admin: " + getUsername();
    }
}