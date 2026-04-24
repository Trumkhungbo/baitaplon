package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;

/**
 * Abstract base class cho người dùng hệ thống.
 * Áp dụng Encapsulation (private fields + getters/setters) và Abstraction.
 */
public abstract class User {

    private long id;
    private String username;
    private String passwordHash;
    private String email;
    private String fullName;
    private UserRole role;
    private long createdAt;

    protected User() {}

    protected User(long id, String username, String passwordHash, String email,
                   String fullName, UserRole role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    // ---- Getters ----

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }
    public long getCreatedAt() { return createdAt; }

    // ---- Setters ----

    public void setId(long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(UserRole role) { this.role = role; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /**
     * Polymorphism: mỗi subclass override để trả về thông tin mô tả riêng.
     */
    public abstract String getDescription();

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', role=%s}", id, username, role);
    }
}
