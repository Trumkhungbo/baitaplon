package com.bidding.server.model.user;

import com.bidding.common.enums.UserRole;

public class Admin extends User {

    public Admin() {
        super();
        setRole(UserRole.ADMIN);
    }

    public Admin(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
        setRole(UserRole.ADMIN);
    }
}
