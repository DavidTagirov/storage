package com.file.storage.dto;

import com.file.storage.ResourceType;

public record ResourceResponse(String path, String name, Long size, ResourceType type) {
}
