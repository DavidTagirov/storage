package com.file.storage.dto;

public record ResourceInfoResponse(String path, String name, Long size, ResourceType type) {
}
