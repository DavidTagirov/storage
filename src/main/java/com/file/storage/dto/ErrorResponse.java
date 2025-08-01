package com.file.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response")
public record ErrorResponse(@Schema(description = "Error message", example = "Resource not found")
                            String message) {
}
