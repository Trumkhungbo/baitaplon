package com.bidding.server.model.user;
import com.bidding.server.model.Entity;
    
// Inheritance: User kế thừa Entity để có id và createdAt
public abstract class User extends Entity {
    private String username;
    private String password;
    private String email;

    public User(String username, String password, String email) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Polymorphism: Bắt buộc lớp con định nghĩa vai trò
    public abstract String getRole();
}