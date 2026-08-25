package com.gymams.dto;

public class AuthResponse {
    private String token;
    private String fullName;
    private String username;
    private String role;
    /** Only present (non-null) for TECHNICIAN accounts — see User.technicianCode. */
    private String technicianCode;

    public AuthResponse(String token, String fullName, String username, String role, String technicianCode) {
        this.token = token;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
        this.technicianCode = technicianCode;
    }

    public String getToken() { return token; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getTechnicianCode() { return technicianCode; }
}
