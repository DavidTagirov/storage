package com.file.storage.controller;

import com.file.storage.dto.ErrorResponse;
import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.exceptions.ResourceAlreadyExistsException;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Resource API", description = "API for managing files and directories")
public class ResourceController {
    private final ResourceService resourceService;

    @Operation(
            summary = "Get resource info",
            description = "Retrieves metadata about a file or directory"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource info retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ResourceInfoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/resource")
    public ResponseEntity<?> getResource(@Parameter(description = "Path to the resource", example = "docs/report.pdf")
                                         @RequestParam String path,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The user is not authorized")); //401
        }
        try {
            ResourceInfoResponse resourceInfoResponse = resourceService.getResourceInfo(path, userDetails.getUsername());
            return ResponseEntity.ok(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or missing path")); //400
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("The resource was not found")); //404
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(
            summary = "Delete resource",
            description = "Deletes a file or empty directory"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Resource deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid path format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/resource")
    public ResponseEntity<?> deleteResource(@Parameter(description = "Path to the resource to delete", example = "temp/old_file.txt")
                                            @RequestParam String path,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The user is not authorized")); //401
        }
        try {
            resourceService.deleteResource(path, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or missing path")); //400
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("The resource was not found")); //404
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(
            summary = "Download resource",
            description = "Downloads a file or directory (as ZIP archive)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File downloaded successfully",
                    content = @Content(schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Invalid path format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/resource/download")
    public ResponseEntity<?> downloadResource(
            @Parameter(description = "Path to the resource", example = "projects/report.pdf")
            @RequestParam String path,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The user is not authorized")); //401
        }
        try {
            InputStream inputStream = resourceService.downloadResource(path, userDetails.getUsername());

            String fileName = path.endsWith("/")
                    ? path.replaceAll("/+$", "").replaceAll("^.*/", "") + ".zip"
                    : path.substring(path.lastIndexOf("/") + 1);

            MediaType contentType = path.endsWith("/")
                    ? MediaType.parseMediaType("application/zip")
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(contentType)
                    .body(new InputStreamResource(inputStream));
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or missing path")); //400
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("The resource was not found")); //404
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(
            summary = "Move/rename resource",
            description = "Moves or renames a file/directory"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource moved successfully",
                    content = @Content(schema = @Schema(implementation = ResourceInfoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Source resource not found"),
            @ApiResponse(responseCode = "409", description = "Target resource already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/resource/move")
    public ResponseEntity<?> moveResource(@Parameter(description = "Current path of the resource", example = "docs/old_name.pdf")
                                              @RequestParam String from,
                                          @Parameter(description = "New path of the resource", example = "archive/new_name.pdf")
                                          @RequestParam String to,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The user is not authorized")); //401
        }
        try {
            ResourceInfoResponse resourceInfoResponse = resourceService.moveResource(from, to, userDetails.getUsername());
            return ResponseEntity.ok(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or missing path")); //400
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("The resource was not found")); //404
        } catch (ResourceAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("The file already exists")); //409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(
            summary = "Search resources",
            description = "Searches files and directories by name"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceInfoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid query"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/resource/search")
    public ResponseEntity<?> searchResource(@Parameter(description = "Search query", example = "report")
                                                @RequestParam String query,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The user is not authorized")); //401
        }
        try {
            List<ResourceInfoResponse> resourceInfoResponse = resourceService.searchResource(query, userDetails.getUsername());
            return ResponseEntity.ok(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or missing path")); //400
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(
            summary = "Upload files",
            description = "Uploads one or multiple files to the specified directory"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Files uploaded successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceInfoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid path"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "File already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResource(@Parameter(description = "Target directory path", example = "uploads/")
                                                @RequestParam String path,
                                            @Parameter(description = "Files to upload",
                                                    content = @Content(mediaType = "multipart/form-data",
                                                            array = @ArraySchema(schema = @Schema(type = "string", format = "binary"))))
                                            @RequestParam("object") List<MultipartFile> files,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The user is not authorized")); //401
        }
        try {
            List<ResourceInfoResponse> resourceInfoResponse = resourceService.uploadResource(path, files, userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or missing path")); //400
        } catch (ResourceAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("The file already exists")); //409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(
            summary = "List directory contents",
            description = "Gets contents of a directory (non-recursive)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Directory contents retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResourceInfoResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid path"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Directory not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/directory")
    public ResponseEntity<?> getDirectory(@Parameter(description = "Directory path", example = "projects/")
                                              @RequestParam String path,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The user is not authorized")); //401
        }
        try {
            List<ResourceInfoResponse> resourceInfoResponse = resourceService.getDirectoryInfo(path, userDetails.getUsername());
            return ResponseEntity.ok(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or missing path")); //400
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("The directory was not found")); //404
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }

    @Operation(
            summary = "Create directory",
            description = "Creates a new empty directory"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Directory created successfully",
                    content = @Content(schema = @Schema(implementation = ResourceInfoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid path"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Parent directory not found"),
            @ApiResponse(responseCode = "409", description = "Directory already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/directory")
    public ResponseEntity<?> createDirectory(@Parameter(description = "Path of the new directory", example = "projects/new_folder/")
                                                 @RequestParam String path,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("The user is not authorized")); //401
        }
        try {
            ResourceInfoResponse resourceInfoResponse = resourceService.createDirectory(path, userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(resourceInfoResponse);
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or missing path")); //400
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("The parent folder does not exist")); //404
        } catch (ResourceAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("The directory already exists")); //409
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal server error")); //500
        }
    }
}