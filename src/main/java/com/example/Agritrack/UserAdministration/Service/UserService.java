package com.example.Agritrack.Service;

import com.example.Agritrack.Model.User;
import com.example.Agritrack.Repository.UserRepository;
import com.example.Agritrack.Security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // 🔹 Register user
    public Map<String, Object> register(User user) {
        Map<String, Object> response = new HashMap<>();

        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            response.put("message", "Username is required");
            return response;
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            response.put("message", "Email is required");
            return response;
        }
        if (user.getMobile() == null || user.getMobile().isEmpty()) {
            response.put("message", "Mobile number is required");
            return response;
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            response.put("message", "Password is required");
            return response;
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            response.put("message", "Email already exists");
            return response;
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            response.put("message", "Username already exists");
            return response;
        }
        if (userRepository.findByMobile(user.getMobile()).isPresent()) {
            response.put("message", "Mobile number already exists");
            return response;
        }

        userRepository.save(user);
        response.put("message", "User registered successfully");
        return response;
    }

    // 🔹 Login user
    public Map<String, Object> login(String identifier, String password) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .or(() -> userRepository.findByMobile(identifier));

        if (userOpt.isEmpty()) {
            response.put("message", "User not found");
            return response;
        }

        User user = userOpt.get();
        if (!user.getPassword().equals(password)) {
            response.put("message", "Invalid password");
            return response;
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        response.put("message", "Login successful");
        response.put("token", token);
        response.put("role", user.getRole());
        return response;
    }

    // 🔹 Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 🔹 Get by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // 🔹 Update
    public Map<String, Object> updateUser(Long id, User userDetails) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            response.put("message", "User not found");
            return response;
        }

        User user = userOpt.get();
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setMobile(userDetails.getMobile());
        user.setPassword(userDetails.getPassword());
        user.setRole(userDetails.getRole());

        userRepository.save(user);
        response.put("message", "User updated successfully");
        return response;
    }

    // 🔹 Delete
    public Map<String, Object> deleteUser(Long id) {
        Map<String, Object> response = new HashMap<>();
        if (!userRepository.existsById(id)) {
            response.put("message", "User not found");
            return response;
        }
        userRepository.deleteById(id);
        response.put("message", "User deleted successfully");
        return response;
    }
}
