package com.example.Agritrack.UserAdministration.Repository;

import com.example.Agritrack.UserAdministration.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMobileNumber(String mobileNumber);
}

