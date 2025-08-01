package com.file.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resource information")
public record ResourceInfoResponse(@Schema(description = "Parent directory path", example = "docs/")
                                   String path,
                                   @Schema(description = "Resource name", example = "report.pdf")
                                   String name,
                                   @Schema(description = "File size in bytes (null for directories)", example = "1024")
                                   Long size,
                                   @Schema(description = "Resource type (FILE or DIRECTORY)", example = "FILE")
                                   ResourceType type) {
}
