package com.file.storage.service;

import com.file.storage.dto.SignInRequest;
import com.file.storage.dto.SignUpRequest;
import com.file.storage.exceptions.UsernameAlreadyExistsException;
import com.file.storage.exceptions.WrongUsernameOrPassword;
import com.file.storage.model.User;
import com.file.storage.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private final String testUsername = "testuser";
    private final String testPassword = "password123";

    @Test
    void signUp_ShouldCreateNewUser() {
        when(userRepository.existsByUsername(testUsername)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn("encodedPassword");

        SignUpRequest request = new SignUpRequest(testUsername, testPassword);
        var response = authService.signUp(request);

        assertEquals(testUsername, response.username());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void signUp_ShouldThrowWhenUsernameExists() {
        when(userRepository.existsByUsername(testUsername)).thenReturn(true);

        SignUpRequest request = new SignUpRequest(testUsername, testPassword);
        assertThrows(UsernameAlreadyExistsException.class, () -> authService.signUp(request));
    }

    @Test
    void signIn_ShouldAuthenticateUser() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(testUsername, testPassword));

        SignInRequest request = new SignInRequest(testUsername, testPassword);
        var response = authService.signIn(request);

        assertEquals(testUsername, response.username());
    }

    @Test
    void signIn_ShouldThrowWhenInvalidCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        SignInRequest request = new SignInRequest(testUsername, "wrongpassword");
        assertThrows(WrongUsernameOrPassword.class, () -> authService.signIn(request));
    }
}