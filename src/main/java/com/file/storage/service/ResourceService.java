package com.file.storage.service;

import com.file.storage.ResourceType;
import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.exceptions.ResourceAlreadyExistsException;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.model.User;
import com.file.storage.repository.UserRepository;
import io.minio.*;
import io.minio.errors.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String fullPath = "user-" + user.getId() + "-files/" + path;
        if (resourceExists(fullPath)) {
            throw new ResourceNotFoundException(fullPath);
        }

        return path.endsWith("/") ? getDirectoryInfo(path) : getFileInfo(fullPath, path);
    }

    private ResourceInfoResponse getFileInfo(String fullPath, String path) {
        return new ResourceInfoResponse(
                extractParentPath(path),
                extractName(path),
                getFileSize(fullPath),
                ResourceType.FILE
        );
    }

    private ResourceInfoResponse getDirectoryInfo(String path) {
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
            if (fullPath.endsWith("/")) {
                return minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(fullPath)
                                .maxKeys(1)
                                .build()
                ).iterator().hasNext();
            } else {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                );
                return true;
            }
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
        } catch (Exception e) {
            throw new ResourceAccessException("Failed to check resource existence");
        }
        return false;
    }

    private void validatePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException("", "Invalid or missing path");
        }
    }

    public void deleteResource(String username, String path) {
        validatePath(path);

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String fullPath = "user-" + user.getId() + "-files/" + path;
        if (resourceExists(fullPath)) {
            throw new ResourceNotFoundException(fullPath);
        }

        if (path.endsWith("/")) {
            deleteDirectory(fullPath);
        } else {
            deleteFile(fullPath);
        }
    }

    private void deleteFile(String fullPath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDirectory(String fullPath) {
        var objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .recursive(true)
                        .build()
        );

        for (var object : objects) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(object.get().objectName())
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public InputStream downloadResource(String username, String path) {
        validatePath(path);

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String fullPath = "user-" + user.getId() + "-files/" + path;
        if (!resourceExists(fullPath)) {
            throw new ResourceNotFoundException(fullPath);
        }

        if (path.endsWith("/")) {
            PipedInputStream pipedInputStream = new PipedInputStream();

            new Thread(() -> {
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(new PipedOutputStream(pipedInputStream))) {
                    addFilesToZip(zipOutputStream, fullPath);
                } catch (IOException e) {
                    try {
                        pipedInputStream.close();
                    } catch (IOException ignored) {
                    }
                    throw new RuntimeException(e);
                }
            }).start();

            return pipedInputStream;
        } else {
            try {
                return minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void addFilesToZip(ZipOutputStream zipOutputStream, String fullPath) {
        var objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .recursive(true)
                        .build()
        );

        for (var o : objects) {
            try {
                if (!o.get().isDir()) {
                    String fileName = o.get().objectName().substring(fullPath.length());
                    zipOutputStream.putNextEntry(new ZipEntry(fileName));

                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(o.get().objectName())
                                    .build()
                    ).transferTo(zipOutputStream);

                    zipOutputStream.closeEntry();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ResourceInfoResponse moveResource(String username, String from, String to) {
        validatePath(from);
        validatePath(to);

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String fullFrom = "user-" + user.getId() + "-files/" + from;
        if (!resourceExists(fullFrom)) {
            throw new ResourceNotFoundException(fullFrom);
        }

        String fullTo = "user-" + user.getId() + "-files/" + to;
        if (resourceExists(fullTo)) {
            throw new ResourceAlreadyExistsException(fullTo);
        }

        if (!from.endsWith("/")) {
            return moveFile(fullFrom, fullTo);
        } else {
            throw new ResourceAlreadyExistsException("You can't move a folder. " + fullTo);
        }
    }

    private ResourceInfoResponse moveFile(String fullFrom, String fullTo) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullTo)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(fullFrom)
                                    .build())
                            .build()
            );

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullFrom)
                            .build()
            );

            return new ResourceInfoResponse(
                    extractParentPath(fullTo),
                    extractName(fullTo),
                    getFileSize(fullTo),
                    ResourceType.FILE
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<ResourceInfoResponse> searchResource(String username, String query) {
        validatePath(query);

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String fullQuery = "user-" + user.getId() + "-files/" + query;

        return searchFile(fullQuery);
    }

    private List<ResourceInfoResponse> searchFile(String fullQuery) {
        List<ResourceInfoResponse> resourceList = new ArrayList<>();

        var objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullQuery)
                        .recursive(true)
                        .build()
        );

        try {
            for (var o : objects) {
                String filePath = o.get().objectName();

                if (filePath.toLowerCase().contains(fullQuery.toLowerCase())) {
                    resourceList.add(new ResourceInfoResponse(
                            extractParentPath(filePath),
                            extractName(filePath),
                            getFileSize(filePath),
                            ResourceType.FILE
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resourceList;
    }

    private Long getFileSize(String fullPath) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build()
            ).size();
        } catch (Exception e) {
            return null;
        }
    }
}
