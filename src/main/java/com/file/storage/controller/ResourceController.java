package com.file.storage.controller;

import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.exceptions.ResourceAlreadyExistsException;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.service.ResourceService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.util.List;

@RestController
@RequestMapping("/api")
@Setter
@Getter
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping({"/resource", "/directory"})
    public ResponseEntity<ResourceInfoResponse> getResourceOrDirectory(@RequestParam String path,
                                                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ResourceInfoResponse resourceInfoResponse = resourceService.getResourceInfo(userDetails.getUsername(), path);
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

    @DeleteMapping("/resource")
    public ResponseEntity<Void> deleteResource(@RequestParam String path,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        try {
            resourceService.deleteResource(userDetails.getUsername(), path);
            return ResponseEntity.noContent().build();
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

    @GetMapping("/resource/download")
    public ResponseEntity<InputStreamResource> downloadResource(@RequestParam String path,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            InputStream inputStream = resourceService.downloadResource(userDetails.getUsername(), path);

            String fileName = path.endsWith("/")
                    ? path.replace("/", "") + ".zip"
                    : path.substring(path.lastIndexOf("/" + 1));

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(inputStream));
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

    @GetMapping("/resource/move")
    public ResponseEntity<ResourceInfoResponse> moveResource(@RequestParam String from,
                                                             @RequestParam String to,
                                                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ResourceInfoResponse resourceInfoResponse = resourceService.moveResource(userDetails.getUsername(), from, to);
            return ResponseEntity.ok(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().build(); //400
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); //401
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build(); //404
        } catch (ResourceAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); //409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();//500
        }
    }

    @GetMapping("/resource/search")
    public ResponseEntity<?> searchResource(@RequestParam String query,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<ResourceInfoResponse> resourceInfoResponse = resourceService.searchResource(userDetails.getUsername(), query);
            return ResponseEntity.ok(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().build(); //400
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); //401
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();//500
        }
    }

    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResource(@RequestParam String path,
                                            @RequestParam("file") List<MultipartFile> files,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            List<ResourceInfoResponse> resourceInfoResponse = resourceService.uploadResource(userDetails.getUsername(), path, files);
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().build(); //400
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); //401
        } catch (ResourceAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); //409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();//500
        }
    }

    @PostMapping("/directory")
    public ResponseEntity<ResourceInfoResponse> createDirectory(@RequestParam String path,
                                                                @AuthenticationPrincipal UserDetails userDetails) {

        try {
            ResourceInfoResponse resourceInfoResponse = resourceService.createDirectory(userDetails.getUsername(), path);
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().build(); //400
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); //401
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build(); //404
        } catch (ResourceAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); //409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();//500
        }
    }
}