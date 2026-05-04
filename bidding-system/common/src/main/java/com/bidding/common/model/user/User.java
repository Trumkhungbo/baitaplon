package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract base class cho người dùng hệ thống.
 * Áp dụng Encapsulation và Inheritance.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "userType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Admin.class,  name = "ADMIN"),
    @JsonSubTypes.Type(value = Seller.class, name = "SELLER"),
    @JsonSubTypes.Type(value = Bidder.class, name = "BIDDER")
})
public abstract class User {

    private long id;
    private String username;
    @JsonIgnore
    private String passwordHash; // không gửi qua mạng sau khi đăng nhập
    private String email;
    private String fullName;
    private UserRole role;
    private long createdAt;

    protected User() {}

    protected User(String username, String passwordHash, String email,
                   String fullName, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    // ---- Getters & Setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** Polymorphism: mỗi subclass mô tả chính mình */
    public abstract String getDescription();

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', role=%s}", id, username, role);
    }
}