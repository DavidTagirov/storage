package com.file.storage.controller;

import com.file.storage.dto.ErrorResponse;
import com.file.storage.dto.SignUpRequest;
import com.file.storage.dto.UserResponse;
import com.file.storage.dto.SignInRequest;
import com.file.storage.exceptions.UnauthorizedUserException;
import com.file.storage.exceptions.UsernameAlreadyExistsException;
import com.file.storage.exceptions.WrongUsernameOrPassword;
import com.file.storage.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            UserResponse userResponse = authService.signUp(signUpRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
        } catch (UsernameAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Username is already occupied")); //409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInRequest signInRequest) {
        try {
            UserResponse userResponse = authService.signIn(signInRequest);
            return ResponseEntity.ok(userResponse);
        } catch (WrongUsernameOrPassword e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("There is no such user or the password is incorrect")); //401
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(HttpServletRequest request) {
        try {
            authService.signOut(request);
            return ResponseEntity.noContent().build();
        } catch (UnauthorizedUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The request is executed by an unauthorized user")); //401
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }
}
