package com.example.Agritrack.UserAdministration.Payloads;


public class LoginRequest {
    private String mobileNumber;
    private String password;
    private String activeRole; // the selected role to continue

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getActiveRole() { return activeRole; }
    public void setActiveRole(String activeRole) { this.activeRole = activeRole; }
}
