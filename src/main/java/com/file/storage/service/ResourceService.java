package com.file.storage.service;

import com.file.storage.dto.ResourceType;
import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.exceptions.ResourceAlreadyExistsException;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.model.User;
import com.file.storage.repository.UserRepository;
import io.minio.*;
import io.minio.errors.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
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
        if (!resourceOrDirectoryExists(fullPath)) {
            throw new ResourceNotFoundException();
        }

        return path.endsWith("/") ? getDirectoryInfo(path) : getFileInfo(fullPath, path);
    }

    private ResourceInfoResponse getFileInfo(String fullPath, String path) {
        return new ResourceInfoResponse(
                getParentPath(path),
                getName(path),
                getFileSize(fullPath),
                ResourceType.FILE
        );
    }

    private ResourceInfoResponse getDirectoryInfo(String path) {
        return new ResourceInfoResponse(
                getParentPath(path),
                getName(path),
                null,
                ResourceType.DIRECTORY
        );
    }

    private String getName(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }

        if (path.endsWith("/")) {
            int penultimateSlash = path.lastIndexOf("/", path.length() - 2);

            if (penultimateSlash <= 0) {
                return path;
            }

            return path.substring(penultimateSlash + 1, path.length() - 2);
        } else {
            int lastSlash = path.lastIndexOf("/");

            if (lastSlash <= 0) {
                return path;
            }

            return path.substring(lastSlash, path.length() - 1);
        }
    }

    private String getParentPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }

        if (path.endsWith("/")) {
            int penultimateSlash = path.lastIndexOf("/", path.length() - 2);

            if (penultimateSlash <= 0) {
                return "";
            }

            return path.substring(0, penultimateSlash + 1);
        } else {
            int lastSlash = path.lastIndexOf("/");

            if (lastSlash <= 0) {
                return "";
            }

            return path.substring(0, lastSlash + 1);
        }
    }

    private boolean resourceOrDirectoryExists(String fullPath) {
        try {
            if (fullPath.endsWith("/")) {
                boolean hasObjectsFolder = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(fullPath)
                                .maxKeys(1)
                                .build()
                ).iterator().hasNext();

                boolean isEmptyFolder = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                ) != null;

                return hasObjectsFolder || isEmptyFolder;
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
        if (!resourceOrDirectoryExists(fullPath)) {
            throw new ResourceNotFoundException();
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
        if (!resourceOrDirectoryExists(fullPath)) {
            throw new ResourceNotFoundException();
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
        if (!resourceOrDirectoryExists(fullFrom)) {
            throw new ResourceNotFoundException();
        }

        String fullTo = "user-" + user.getId() + "-files/" + to;
        if (resourceOrDirectoryExists(fullTo)) {
            throw new ResourceAlreadyExistsException();
        }

        if (!from.endsWith("/")) {
            return moveFile(fullFrom, fullTo);
        } else {
            throw new ResourceAlreadyExistsException();
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
                    getParentPath(fullTo),
                    getName(fullTo),
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
                            getParentPath(filePath),
                            getName(filePath),
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

    public List<ResourceInfoResponse> uploadResource(String username, String path, List<MultipartFile> files) {
        validatePath(path);

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String fullPath = "user-" + user.getId() + "-files/" + path;

        List<ResourceInfoResponse> resourceList = new ArrayList<>();
        for (MultipartFile file : files) {
            fullPath += file.getOriginalFilename();
            fullPath = fullPath.replaceAll("/+", "/");

            if (resourceOrDirectoryExists(fullPath)) {
                throw new ResourceAlreadyExistsException();
            }

            createParentDirectories(fullPath);

            try {
                resourceList.add(uploadFile(fullPath, file.getInputStream(), file.getSize()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return resourceList;
    }

    private void createParentDirectories(String fullPath) {
        List<String> folders = List.of(fullPath.split("/"));
        StringBuilder builderPath = new StringBuilder();

        for (String folder : folders) {
            builderPath.append(folder).append("/");

            if (!resourceOrDirectoryExists(builderPath.toString())) {
                createEmptyDirectory(builderPath.toString());
            }
        }
    }

    private void createEmptyDirectory(String path) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResourceInfoResponse uploadFile(String fullPath, InputStream inputStream, long fileSize) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .stream(inputStream, fileSize, -1)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ResourceInfoResponse(
                getParentPath(fullPath.substring(0, fullPath.lastIndexOf("/"))),
                getName(fullPath.substring(fullPath.lastIndexOf("/"))),
                fileSize,
                ResourceType.FILE
        );
    }

    public ResourceInfoResponse createDirectory(String username, String path) {
        validateDirectoryPath(path);

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String fullPath = "user-" + user.getId() + "-files/" + path;
        String parentPath = getParentPath(fullPath);

        if (!resourceOrDirectoryExists(parentPath)) {
            throw new ResourceNotFoundException();
        }
        if (resourceOrDirectoryExists(fullPath)) {
            throw new ResourceAlreadyExistsException();
        }

        createEmptyDirectory(fullPath);

        return new ResourceInfoResponse(
                parentPath,
                getName(fullPath),
                null,
                ResourceType.DIRECTORY);
    }

    private void validateDirectoryPath(String path) {
        if (path == null || !path.endsWith("/")) {
            throw new InvalidPathException("", "Invalid or missing path");
        }
    }
}
