package com.file.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        log.info("--- MinIO Configuration ---");
        log.info("URL: {}", url);
        log.info("Access Key: {}", accessKey);
        log.info("Bucket Name: {}", bucketName);
        log.info("---------------------------");

        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(url)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );

            if (!isExist) {
                log.info("Bucket '{}' not found. Attempting to create it", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
                log.info("Bucket '{}' created successfully", bucketName);
            } else {
                log.info("Bucket '{}' already exists", bucketName);
            }

            return minioClient;
        } catch (Exception e) {
            log.error("Could not connect to MinIO", e);
            throw new RuntimeException("Ошибка при подключении к MinIO: " + e.getMessage(), e);
        }
    }
}
