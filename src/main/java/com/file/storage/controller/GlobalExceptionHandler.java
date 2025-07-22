/*
package com.file.storage.controller;

import com.file.storage.dto.ErrorResponse;
import com.file.storage.exceptions.ResourceAlreadyExistsException;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.exceptions.WrongUsernameOrPassword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.InvalidPathException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("The resource or directory was not found"));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(ResourceAlreadyExistsException e) {
        log.warn("Resource already exists: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("The resource already exists"));
    }

    @ExceptionHandler(WrongUsernameOrPassword.class)
    public ResponseEntity<ErrorResponse> handleWrongCredentials(WrongUsernameOrPassword e) {
        log.warn("Invalid login attempt: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid username or password"));
    }

    @ExceptionHandler({InvalidPathException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleInvalidPath(Exception e) {
        log.warn("Invalid path or argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Invalid request parameter: " + e.getMessage()));
    }

    // Этот обработчик перехватит все остальные ошибки и вернет 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllOtherExceptions(Exception e) {
        log.error("An unexpected error occurred", e); // Логируем полный stack trace
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("An internal server error occurred. Please check the logs."));
    }
}
*/
