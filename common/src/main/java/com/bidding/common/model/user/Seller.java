package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;

public class Seller extends User {

    public Seller() {
        super();
        setRole(UserRole.SELLER);
    }

    public Seller(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
        setRole(UserRole.SELLER);
    }
}
