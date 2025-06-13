package com.file.storage.dto;

import jakarta.validation.constraints.Size;

public record UserResponse(
        @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters") String username) {
}
