package com.example.Agritrack.UserAdministration.Service;

import com.example.Agritrack.UserAdministration.Model.User;
import com.example.Agritrack.UserAdministration.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    // ✅ Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ✅ Get user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // ✅ Update partial user (PATCH)
    public User updateUserPartial(Long id, User updatedUser) {
        User existingUser = getUserById(id);

        if (updatedUser.getName() != null) {
            existingUser.setName(updatedUser.getName());
        }
        if (updatedUser.getEmail() != null) {
            existingUser.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getMobileNumber() != null) {
            // Prevent duplicate mobile number update
            if (userRepository.findByMobileNumber(updatedUser.getMobileNumber()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number already in use!");
            }
            existingUser.setMobileNumber(updatedUser.getMobileNumber());
        }

        return userRepository.save(existingUser);
    }

    // ✅ Delete user by ID
    public String deleteUser(Long id) {
        User existingUser = getUserById(id);
        userRepository.delete(existingUser);
        return "User deleted successfully!";
    }
}
