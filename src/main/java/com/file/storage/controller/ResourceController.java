package com.file.storage.controller;

import com.file.storage.dto.ResourceResponse;
import com.file.storage.service.ResourceService;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.security.Principal;

@RestController
@RequestMapping("/api/resource")
@Setter
@Getter
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ResponseEntity<ResourceResponse> getResource(@RequestParam String path, Principal principal) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().build(); //400
        }
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();//401
        }
        try {

            InputStream inputStream = resourceService.downloadFile(path);
            return ResponseEntity.ok().body(new ResourceResponse(inputStream));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();//500
        }
    }

    public String getParentPath(String path) {

    }
}