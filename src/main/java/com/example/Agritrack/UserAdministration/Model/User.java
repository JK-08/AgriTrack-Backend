package com.example.Agritrack.UserAdministration.Model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "USERS_INFO") // 👈 matches the DB table name exactly
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "MOBILE_NUMBER", nullable = false, unique = true)
    private String mobileNumber;

    @Column(name = "EMAIL", nullable = true , unique = true)
    private String email;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    // ✅ Separate role table mapping
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "USER_ROLES_INFO",
            joinColumns = @JoinColumn(name = "USER_ID")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE")
    private Set<Role> roles = new HashSet<>();

    public enum Role {
        ADMIN,
        EMPLOYEE,
        USER
    }

    // ---------- Getters & Setters ----------
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
