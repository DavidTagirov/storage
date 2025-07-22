package com.file.storage.service;

import com.file.storage.dto.SignInRequest;
import com.file.storage.dto.SignUpRequest;
import com.file.storage.dto.UserResponse;
import com.file.storage.exceptions.UsernameAlreadyExistsException;
import com.file.storage.exceptions.WrongUsernameOrPassword;
import com.file.storage.model.User;
import com.file.storage.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse signUp(SignUpRequest request, HttpServletRequest httpRequest) {
        log.info("Attempting to register user: {}", request.username());
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Username already exists: {}", request.username());
            throw new UsernameAlreadyExistsException();
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        log.info("User registered successfully: {}", user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        httpRequest.getSession(true);
        log.debug("Created new session for user: {}", request.username());

        return new UserResponse(user.getUsername());
    }

    public UserResponse signIn(SignInRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            httpRequest.getSession(true);
            log.debug("Created new session for user: {}", request.username());


            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Создаем новую сессию
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);

            return new UserResponse(request.username());
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt for user: {}", request.username());
            throw new WrongUsernameOrPassword();
        }
    }

    public void signOut(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
            log.debug("Session invalidated for user");
        }
    }
}