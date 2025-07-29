package com.file.storage.service;

import com.file.storage.dto.ResourceType;
import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.exceptions.ResourceAlreadyExistsException;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.model.User;
import com.file.storage.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ResourceService {
    private final UserRepository userRepository;
    private final MinioService minioService;

    public ResourceService(
            UserRepository userRepository,
            MinioService minioService) {
        this.userRepository = userRepository;
        this.minioService = minioService;
    }

    @PostConstruct
    public void initUserFolders() {
        userRepository.findAll().forEach(user -> {
            String userFolder = "user-" + user.getId() + "-files/";
            if (!minioService.resourceOrDirectoryExists(userFolder)) {
                createEmptyDirectory(userFolder);
            }
        });
        minioService.ensureBucketExists();
    }

    public ResourceInfoResponse getResourceInfo(String path, String username) {
        path = validatePath(path, username);

        if (path.endsWith("/")) {
            throw new InvalidPathException("", "Its cannot be a directory");
        }
        if (!minioService.resourceOrDirectoryExists(path)) {
            throw new ResourceNotFoundException();
        }
        return new ResourceInfoResponse(
                getParentPath(path),
                getName(path),
                minioService.statObject(path).size(),
                ResourceType.FILE
        );
    }

    private String getName(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            throw new InvalidPathException("", "Empty path");
        }
        int lastSlash = path.lastIndexOf("/", path.length() - 2);
        if (lastSlash <= 0) {
            return path;
        }
        return path.substring(lastSlash + 1);
    }

    private String getParentPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }
        path = path.replaceFirst("^user-\\d+-files/", "");

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

    public void deleteResource(String path, String username) throws Exception {
        path = validatePath(path, username);

        if (!minioService.resourceOrDirectoryExists(path)) {
            throw new ResourceNotFoundException();
        }
        if (path.endsWith("/")) {
            deleteDirectory(path);
        } else {
            minioService.removeObject(path);
        }
    }

    public InputStream downloadResource(String path, String username) {
        path = validatePath(path, username);

        if (!minioService.resourceOrDirectoryExists(path)) {
            throw new ResourceNotFoundException();
        }
        if (path.endsWith("/")) {
            PipedInputStream pipedInputStream = new PipedInputStream();
            String finalPath = path;

            new Thread(() -> {
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(new PipedOutputStream(pipedInputStream))) {
                    addFilesToZip(zipOutputStream, finalPath);
                } catch (Exception e) {
                    try {
                        pipedInputStream.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }).start();
            return pipedInputStream;
        } else {
            return minioService.getObject(path);
        }
    }

    private void addFilesToZip(ZipOutputStream zipOutputStream, String fullPath) throws Exception {
        var objects = minioService.listObjects(fullPath, null, true);

        for (var object : objects) {
            if (!object.get().isDir()) {
                String fileName = object.get().objectName().substring(fullPath.length());
                zipOutputStream.putNextEntry(new ZipEntry(fileName));
                minioService.getObject(object.get().objectName()).transferTo(zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }
    }

    public ResourceInfoResponse moveResource(String from, String to, String username) {
        from = validatePath(from, username);
        to = validatePath(to, username);

        if (!minioService.resourceOrDirectoryExists(from)) {
            throw new ResourceNotFoundException();
        }
        if (minioService.resourceOrDirectoryExists(to)) {
            throw new ResourceAlreadyExistsException();
        }
        minioService.copyObject(from, to);
        minioService.removeObject(from);

        return new ResourceInfoResponse(
                getParentPath(to),
                getName(to),
                minioService.statObject(to).size(),
                ResourceType.FILE
        );
    }

    public List<ResourceInfoResponse> searchResource(String query, String username) throws Exception {
        query = validatePath(query, username);

        var objects = minioService.listObjects(query, null, true);
        List<ResourceInfoResponse> resourceList = new ArrayList<>();

        for (var object : objects) {
            String filePath = object.get().objectName();

            if (filePath.toLowerCase().contains(query.toLowerCase())) {
                resourceList.add(new ResourceInfoResponse(
                        getParentPath(filePath),
                        getName(filePath),
                        minioService.statObject(filePath).size(),
                        ResourceType.FILE
                ));
            }
        }
        return resourceList;
    }

    public List<ResourceInfoResponse> uploadResource(String path, List<MultipartFile> files, String username) {
        path = validateDirectoryPath(path, username);
        List<ResourceInfoResponse> resourceList = new ArrayList<>();

        for (MultipartFile file : files) {
            String fullPath = path + file.getOriginalFilename();

            if (minioService.resourceOrDirectoryExists(fullPath)) {
                throw new ResourceAlreadyExistsException();
            }
            createParentDirectories(path);

            try (InputStream inputStream = file.getInputStream()) {
                minioService.putObject(fullPath, inputStream, file.getSize(), -1);
                resourceList.add(new ResourceInfoResponse(
                        getParentPath(fullPath),
                        getName(fullPath),
                        file.getSize(),
                        ResourceType.FILE
                ));
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }
        return resourceList;
    }

    private void createParentDirectories(String path) {
        String[] parts = path.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            currentPath.append(part).append("/");
            String dirPath = currentPath.toString();

            if (!minioService.resourceOrDirectoryExists(dirPath)) {
                createEmptyDirectory(dirPath);
            }
        }
    }

    private void createEmptyDirectory(String path) {
        minioService.putObject(path, new ByteArrayInputStream(new byte[0]), 0, -1);
    }

    public List<ResourceInfoResponse> getDirectoryInfo(String path, String username) throws Exception {
        path = validateDirectoryPath(path, username);

        if (!minioService.resourceOrDirectoryExists(path)) {
            throw new ResourceNotFoundException();
        }
        var objects = minioService.listObjects(path, "/", false);
        List<ResourceInfoResponse> resourceList = new ArrayList<>();

        for (var object : objects) {
            ResourceInfoResponse resource;

            if (object.get().objectName().equals(path)) {
                continue;
            }
            if (object.get().isDir()) {
                resource = new ResourceInfoResponse(
                        getParentPath(path),
                        getName(path),
                        null,
                        ResourceType.DIRECTORY
                );
            } else {
                resource = new ResourceInfoResponse(
                        getParentPath(path),
                        getName(path),
                        minioService.statObject(object.get().objectName()).size(),
                        ResourceType.FILE
                );
            }
            if (resource != null) {
                resourceList.add(resource);
            }
        }
        return resourceList;
    }

    public ResourceInfoResponse createDirectory(String path, String username) {
        path = validateDirectoryPath(path, username);

        if (!getParentPath(path).isEmpty() && !minioService.resourceOrDirectoryExists(getParentPath(path))) {
            throw new ResourceNotFoundException();
        }
        if (minioService.resourceOrDirectoryExists(path)) {
            throw new ResourceAlreadyExistsException();
        }
        createEmptyDirectory(path);

        return new ResourceInfoResponse(
                getParentPath(path),
                getName(path),
                null,
                ResourceType.DIRECTORY);
    }

    private String validatePath(String path, String username) {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException("", "Invalid or missing path");
        }
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ("user-" + user.getId() + "-files/" + path).replaceAll("/+", "/");
    }

    private String validateDirectoryPath(String path, String username) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ("user-" + user.getId() + "-files/" + path).replaceAll("/+", "/");
    }

    public void deleteDirectory(String fullPath) throws Exception {
        var objects = minioService.listObjects(fullPath, null, true);

        for (var object : objects) {
            minioService.removeObject(object.get().objectName());
        }
    }
}