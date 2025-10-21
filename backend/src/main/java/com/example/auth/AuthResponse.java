package com.example.auth;

import java.util.List;

public class AuthResponse {
    private String token;
    private Long expiresAt; // unix millis
    private List<String> permissions;

    public AuthResponse() {}

    public AuthResponse(String token, Long expiresAt, List<String> permissions) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.permissions = permissions;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "token='" + token + '\'' +
                ", expiresAt=" + expiresAt +
                ", permissions=" + permissions +
                '}';
    }
}
