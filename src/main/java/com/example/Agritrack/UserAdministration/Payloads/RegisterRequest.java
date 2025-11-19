package com.example.Agritrack.UserAdministration.Payloads;


import java.util.Set;

public class RegisterRequest {
    private String name;
    private String mobileNumber;
    private String email;
    private String password;
    private Set<String> roles; // user can select multiple

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
