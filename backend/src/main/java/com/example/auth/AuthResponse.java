// AuthResponse.java
package com.example.auth;

public class AuthResponse {
    private String token;
    private Long expiresAt; // unix millis, optional

    public AuthResponse() {}
    public AuthResponse(String token, Long expiresAt) {
        this.token = token; this.expiresAt = expiresAt;
    }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
}