package com.file.storage.service;

import com.file.storage.ResourceType;
import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.model.User;
import com.file.storage.repository.UserRepository;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.nio.file.InvalidPathException;

@Service
@Setter
@Getter
public class ResourceService {
    private final MinioClient minioClient;
    private final UserRepository userRepository;
    private final String bucketName;

    public ResourceService(MinioClient minioClient,
                           UserRepository userRepository,
                           @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.userRepository = userRepository;
        this.bucketName = bucketName;
    }

    public ResourceInfoResponse getResourceInfo(String username, String path) {
        validatePath(path);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String fullPath = "user-" + user.getId() + "-files/" + path;
        if (!resourceExists(fullPath)) {
            throw new ResourceNotFoundException(fullPath);
        }

        return path.endsWith("/") ? getDirectoryInfo(fullPath, path) : getFileInfo(fullPath, path);
    }

    private ResourceInfoResponse getFileInfo(String fullPath, String path) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build()
            );

            return new ResourceInfoResponse(
                    extractParentPath(path),
                    extractName(path),
                    stat.size(),
                    ResourceType.FILE
            );
        } catch (Exception e) {
            throw new ResourceAccessException(e.getMessage());
        }
    }

    private ResourceInfoResponse getDirectoryInfo(String fullPath, String path) {
        return new ResourceInfoResponse(
                extractParentPath(path),
                extractName(path),
                null,
                ResourceType.DIRECTORY
        );
    }

    private String extractName(String path) {
        int index = path.lastIndexOf("/");
        if (index == -1) {
            return path;
        }
        return path.substring(index + 1);
    }

    private String extractParentPath(String path) {
        int index = path.lastIndexOf("/");
        if (index <= 0) {
            return "";
        }
        return path.substring(0, index);
    }

    private boolean resourceExists(String fullPath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPath)
                    .build());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
        } catch (Exception e) {
            throw new ResourceAccessException("Failed to check resource existence", e);
        }
        return true;
    }

    private void validatePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException("", "Invalid or missing path");
        }
    }
}
