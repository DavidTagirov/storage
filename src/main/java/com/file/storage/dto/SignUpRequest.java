package com.file.storage.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(@NotBlank @Size(min = 3, max = 50) String username,
                            @NotBlank @Size(min = 6, max = 50) String password) {
}
