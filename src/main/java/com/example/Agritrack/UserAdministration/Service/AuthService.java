package com.example.Agritrack.UserAdministration.Service;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.Agritrack.UserAdministration.Payloads.*;
import com.example.Agritrack.UserAdministration.Model.User;
import com.example.Agritrack.UserAdministration.Model.User.Role;
import com.example.Agritrack.UserAdministration.Repository.UserRepository;
import com.example.Agritrack.UserAdministration.Security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ REGISTER
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists by mobile or email
        if (userRepository.findByMobileNumber(request.getMobileNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number already registered!");
        }

        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setMobileNumber(request.getMobileNumber());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // encrypt password

        // Assign roles (default = USER)
        Set<Role> roleSet = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String r : request.getRoles()) {
                try {
                    roleSet.add(Role.valueOf(r.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + r);
                }
            }
        } else {
            roleSet.add(Role.USER);
        }
        user.setRoles(roleSet);

        // Save new user
        userRepository.save(user);

        // Generate JWT token
        String activeRole = roleSet.iterator().next().name();
        String token = jwtUtil.generateToken(user.getMobileNumber(), roleSet, activeRole);

        return new AuthResponse(token, true, getRoleNames(roleSet), activeRole, "Registration successful");
    }

    // ✅ LOGIN
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        String activeRole = request.getActiveRole();
        if (activeRole != null && !activeRole.isEmpty()) {
            activeRole = activeRole.toUpperCase();

            // Create a new final variable for use in the lambda
            final String roleToCheck = activeRole;

            boolean hasRole = user.getRoles().stream()
                    .anyMatch(role -> role.name().equalsIgnoreCase(roleToCheck));

            if (!hasRole) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User does not have role: " + activeRole);
            }
        }

        String token = jwtUtil.generateToken(user.getMobileNumber(), user.getRoles(), activeRole);
        return new AuthResponse(token, true, getRoleNames(user.getRoles()), activeRole, "Login successful");
    }

    private Set<String> getRoleNames(Set<Role> roles) {
        return roles.stream().map(Enum::name).collect(Collectors.toSet());
    }
}
