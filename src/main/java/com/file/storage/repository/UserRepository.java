package com.file.storage.repository;

import com.file.storage.model.User;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByPassword(String password);

    User findByUsernameAndPassword(
            @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters") String username,
            @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters") String password
    );

    boolean existUserByUsernameAndPassword(String username, String password);
}
