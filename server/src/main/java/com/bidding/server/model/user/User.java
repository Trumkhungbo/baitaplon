package com.bidding.server.model.user;

import com.bidding.common.enums.UserRole;

public abstract class User extends com.bidding.server.model.Entity {
    private String username;
    private String passwordHash;
    private String email;
    private String fullName;
    private UserRole role;

    public User() {
        super();
    }

    public User(String username, String passwordHash, String email) {
        super();
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
