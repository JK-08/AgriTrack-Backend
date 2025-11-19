package com.example.Agritrack.UserAdministration.dto;


import java.util.Set;

public class RegisterRequest {
    private String name;
    private String mobileNumber;
    private String email;
    private String password;
    private Set<String> roles; // optional (multiple)
}
