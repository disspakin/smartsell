package com.smartsell.dto;

public class AuthResponse {

    private String token;
    private String email;
    private String displayName;
    private String role;

    public AuthResponse(String token, String email, String displayName, String role) {
        this.token = token;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
