package com.file.storage.controller;

import com.file.storage.dto.ErrorResponse;
import com.file.storage.dto.SignUpRequest;
import com.file.storage.dto.UserResponse;
import com.file.storage.dto.SignInRequest;
import com.file.storage.exceptions.UnauthorizedUserException;
import com.file.storage.exceptions.UsernameAlreadyExistsException;
import com.file.storage.exceptions.WrongUsernameOrPassword;
import com.file.storage.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "API for user authentication and registration")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Register new user", description = "Creates a new user account and returns user data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Username already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest signUpRequest, HttpServletRequest request) { // <-- Добавлен HttpServletRequest
        try {
            UserResponse userResponse = authService.signUp(signUpRequest, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
        } catch (UsernameAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Username is already occupied")); //409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(summary = "Authenticate user", description = "Authenticates user and creates a new session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInRequest signInRequest, HttpServletRequest request) {
        try {
            UserResponse userResponse = authService.signIn(signInRequest, request);
            return ResponseEntity.ok(userResponse);
        } catch (WrongUsernameOrPassword e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("There is no such user or the password is incorrect")); //401
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(summary = "Logout user", description = "Terminates the current user session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User successfully logged out"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
