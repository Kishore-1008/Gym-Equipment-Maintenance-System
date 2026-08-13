package com.gymams.dto;

public class AuthResponse {
    private String token;
    private String fullName;
    private String username;
    private String role;

    public AuthResponse(String token, String fullName, String username, String role) {
        this.token = token;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}
