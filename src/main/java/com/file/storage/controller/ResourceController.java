package com.file.storage.controller;

import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.service.ResourceService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import java.nio.file.InvalidPathException;

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
    public ResponseEntity<ResourceInfoResponse> getResource(@RequestParam String path,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ResourceInfoResponse resourceInfoResponse = resourceService.getResourceInfo(
                    userDetails.getUsername(),
                    path);
            return ResponseEntity.ok(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().build(); //400
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); //401
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build(); //404
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();//500
        }
    }
}