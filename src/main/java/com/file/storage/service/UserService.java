package com.file.storage.service;

import com.file.storage.dto.SignInRequest;
import com.file.storage.dto.SignUpRequest;
import com.file.storage.dto.UserResponse;
import com.file.storage.exceptions.UnauthorizedUser;
import com.file.storage.exceptions.UsernameAlreadyExistsException;
import com.file.storage.exceptions.WrongUsernameOrPassword;
import com.file.storage.model.User;
import com.file.storage.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private User user;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse signUp(SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.username())) {
            throw new UsernameAlreadyExistsException();
        }

        user = new User();
        user.setUsername(signUpRequest.username());
        user.setPassword(passwordEncoder.encode(signUpRequest.password()));
        user = userRepository.save(user);
        return new UserResponse(user.getUsername());
    }

    public UserResponse signIn(SignInRequest signInRequest) {
        if (!userRepository.existUserByUsernameAndPassword(signInRequest.username(), signInRequest.password())) {
            throw new WrongUsernameOrPassword();
        }
        User user = userRepository.findByUsernameAndPassword(signInRequest.username(), signInRequest.password());
        return new UserResponse(user.getUsername());
    }

    public void signOut() {
        if (user == null) {
            throw new UnauthorizedUser();
        }
        user = null;
    }

    /*public UserResponse getUserById(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserResponse(user.getUsername());
    }*/


    public UserResponse getMe() {
        if (user == null) {
            throw new UnauthorizedUser();
        }
        return new UserResponse(user.getUsername());
    }
}
