package com.bidding.common.payload;

import com.bidding.common.enums.UserRole;

public class LoginResponse {
    private String token;
    private String username;
    private UserRole role;
    private long userId;

   
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
//  trả về đăng nhập thành công và lưu thông tin lại