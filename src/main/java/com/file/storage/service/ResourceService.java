package com.file.storage.service;

import com.file.storage.dto.ResourceResponse;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Setter
@Getter
public class ResourceService {
    private final MinioClient minioClient;
    private String bucketName;

    public ResourceService(MinioClient minioClient,
                           @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public String uploadFile(Long userId, MultipartFile file) {

    }

    public ResourceResponse downloadFile(String path, Long userId) {
        MinioClient minioClient = minioClient.downloadObject(
                DownloadObjectArgs.builder()
                        .bucket(bucketName)
                        .object("user-" + userId + "-files" + path)
                        .build()
        );
    }
}
