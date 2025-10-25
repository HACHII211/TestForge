package com.example.auth;

import java.util.List;

public class AuthResponse {
    private String token;
    private Long expiresAt; // unix millis
    private List<String> permissions;
    private UserDTO user; // 当前用户信息（不含密码）

    public AuthResponse() {}

    public AuthResponse(String token, Long expiresAt, List<String> permissions, UserDTO user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.permissions = permissions;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "token='" + token + '\'' +
                ", expiresAt=" + expiresAt +
                ", permissions=" + permissions +
                ", user=" + user +
                '}';
    }
}
