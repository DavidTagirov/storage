package com.file.storage.service;

import com.file.storage.dto.ResourceType;
import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.exceptions.ResourceAlreadyExistsException;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.model.User;
import com.file.storage.repository.MinioRepository;
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
    private final MinioRepository minioRepository;

    public ResourceService(
            UserRepository userRepository,
            MinioRepository minioRepository) {
        this.userRepository = userRepository;
        this.minioRepository = minioRepository;
    }

    @PostConstruct
    public void initUserFolders() {
        minioRepository.ensureBucketExists();

        userRepository.findAll().forEach(user -> {
            String userFolder = "user-" + user.getId() + "-files/";

            if (!minioRepository.resourceOrDirectoryExists(userFolder)) {
                createEmptyDirectory(userFolder);
            }
        });
    }

    public ResourceInfoResponse getResourceInfo(String path, String username) {
        path = validatePath(path, username);

        if (path.endsWith("/")) {
            throw new InvalidPathException("", "Its cannot be a directory");
        }
        if (!minioRepository.resourceOrDirectoryExists(path)) {
            throw new ResourceNotFoundException();
        }

        return new ResourceInfoResponse(
                getParentPath(path),
                getName(path),
                minioRepository.statObject(path).size(),
                ResourceType.FILE
        );
    }

    public void deleteResource(String path, String username) {
        path = validatePath(path, username);

        if (!minioRepository.resourceOrDirectoryExists(path)) {
            throw new ResourceNotFoundException();
        }

        if (path.endsWith("/")) {
            deleteDirectory(path);
        } else {
            minioRepository.removeObject(path);
        }
    }

    private void deleteDirectory(String fullPath) {
        var objects = minioRepository.listObjects(fullPath, null, true);

        for (var object : objects) {
            try {
                minioRepository.removeObject(object.get().objectName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public InputStream downloadResource(String path, String username) throws IOException {
        path = validatePath(path, username);

        if (!minioRepository.resourceOrDirectoryExists(path)) {
            throw new ResourceNotFoundException();
        }

        if (path.endsWith("/")) {
            return downloadAsZip(path);
        } else {
            return minioRepository.getObject(path);
        }
    }

    private InputStream downloadAsZip(String path) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);

        new Thread(() -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
                addFilesToZip(zipOut, path);
            } catch (Exception e) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }).start();

        return in;
    }

    private void addFilesToZip(ZipOutputStream zipOutputStream, String fullPath) {
        var objects = minioRepository.listObjects(fullPath, null, true);

        for (var object : objects) {
            try {
                if (!object.get().isDir()) {
                    String fileName = object.get().objectName().substring(fullPath.length());
                    zipOutputStream.putNextEntry(new ZipEntry(fileName));

                    minioRepository.getObject(object.get().objectName()).transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ResourceInfoResponse moveResource(String from, String to, String username) {
        from = validatePath(from, username);
        to = validatePath(to, username);

        if (!minioRepository.resourceOrDirectoryExists(from)) {
            throw new ResourceNotFoundException();
        }
        if (minioRepository.resourceOrDirectoryExists(to)) {
            throw new ResourceAlreadyExistsException();
        }

        minioRepository.copyObject(from, to);
        minioRepository.removeObject(from);

        return new ResourceInfoResponse(
                getParentPath(to),
                getName(to),
                minioRepository.statObject(to).size(),
                ResourceType.FILE
        );
    }

    public List<ResourceInfoResponse> searchResource(String query, String username) {
        query = validatePath(query, username);

        var objects = minioRepository.listObjects(query, null, true);

        List<ResourceInfoResponse> resourceList = new ArrayList<>();
        for (var object : objects) {
            try {
                String filePath = object.get().objectName();
                if (filePath.toLowerCase().contains(query.toLowerCase())) {
                    resourceList.add(new ResourceInfoResponse(
                            getParentPath(filePath),
                            getName(filePath),
                            minioRepository.statObject(filePath).size(),
                            ResourceType.FILE
                    ));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return resourceList;
    }

    public List<ResourceInfoResponse> uploadResource(String path, List<MultipartFile> files, String username) {
        path = validateDirectoryPath(path, username);

        List<ResourceInfoResponse> resourceList = new ArrayList<>();

        for (MultipartFile file : files) {
            String fullPath = path + file.getOriginalFilename();

            if (minioRepository.resourceOrDirectoryExists(fullPath)) {
                throw new ResourceAlreadyExistsException();
            }

            createParentDirectories(path);

            try (InputStream inputStream = file.getInputStream()) {
                minioRepository.putObject(fullPath, inputStream, file.getSize(), -1);

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

            if (!minioRepository.resourceOrDirectoryExists(dirPath)) {
                createEmptyDirectory(dirPath);
            }
        }
    }

    public List<ResourceInfoResponse> getDirectoryInfo(String path, String username) {
        path = validateDirectoryPath(path, username);

        if (!minioRepository.resourceOrDirectoryExists(path)) {
            throw new ResourceNotFoundException();
        }

        var objects = minioRepository.listObjects(path, "/", false);

        List<ResourceInfoResponse> resourceList = new ArrayList<>();

        for (var object : objects) {
            try {
                ResourceInfoResponse resource;
                String objectName = object.get().objectName();

                if (objectName.equals(path) || (objectName + "/").equals(path)) {
                    continue;
                }

                if (object.get().isDir()) {
                    resource = new ResourceInfoResponse(
                            path.replaceFirst("^user-\\d+-files/", ""),
                            objectName.substring(path.length()),
                            null,
                            ResourceType.DIRECTORY
                    );
                } else {
                    resource = new ResourceInfoResponse(
                            path.replaceFirst("^user-\\d+-files/", ""),
                            objectName.substring(path.length()),
                            minioRepository.statObject(objectName).size(),
                            ResourceType.FILE
                    );
                }
                resourceList.add(resource);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return resourceList;
    }

    public ResourceInfoResponse createDirectory(String path, String username) {
        path = validateDirectoryPath(path, username);

        if (!getParentPath(path).isEmpty() && !minioRepository.resourceOrDirectoryExists(getParentPath(path))) {
            throw new ResourceNotFoundException();
        }
        if (minioRepository.resourceOrDirectoryExists(path)) {
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
            throw new InvalidPathException("", "Invalid path");
        }
        path = path.replaceFirst("^user-\\d+-files/", "");

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return ("user-" + user.getId() + "-files/" + path)
                .replaceAll("/+", "/")
                .replaceAll(" +", "")
                .trim();
    }

    private String validateDirectoryPath(String path, String username) {
        path = path.replaceFirst("^user-\\d+-files/", "");

        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return ("user-" + user.getId() + "-files/" + path + "/")
                .replaceAll("/+", "/")
                .replaceAll(" +", "")
                .trim();
    }

    private String getName(String path) {
        path = path.replaceFirst("^user-\\d+-files/", "");

        if (path.isEmpty() || path.equals("/")) {
            throw new InvalidPathException("", "Empty path");
        }

        int lastSlash = path.lastIndexOf("/", path.length() - 2);

        return lastSlash <= 0 ? path : path.substring(lastSlash + 1);
    }

    private String getParentPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }

        path = path.replaceFirst("^user-\\d+-files/", "");

        if (path.endsWith("/")) {
            int penultimateSlash = path.lastIndexOf("/", path.length() - 2);

            return penultimateSlash <= 0 ? "" : path.substring(0, penultimateSlash + 1);
        } else {
            int lastSlash = path.lastIndexOf("/");

            return lastSlash <= 0 ? "" : path.substring(0, lastSlash + 1);
        }
    }

    private void createEmptyDirectory(String path) {
        minioRepository.putObject(path, new ByteArrayInputStream(new byte[0]), 0, -1);
    }
}