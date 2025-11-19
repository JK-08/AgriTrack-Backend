package com.example.Agritrack.UserAdministration.Payloads;


import java.util.Set;

public class AuthResponse {
    private String token;
    private boolean isAuthenticated;
    private Set<String> roles;
    private String activeRole;
    private String message;

    public AuthResponse(String token, boolean isAuthenticated, Set<String> roles, String activeRole, String message) {
        this.token = token;
        this.isAuthenticated = isAuthenticated;
        this.roles = roles;
        this.activeRole = activeRole;
        this.message = message;
    }

    // Getters
    public String getToken() { return token; }
    public boolean isAuthenticated() { return isAuthenticated; }
    public Set<String> getRoles() { return roles; }
    public String getActiveRole() { return activeRole; }
    public String getMessage() { return message; }
}
