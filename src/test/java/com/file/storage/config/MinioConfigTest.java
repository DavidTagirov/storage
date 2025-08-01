package com.file.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Tag("integration")
@Tag("minio")
class MinioConfigTest {

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio")
            .withCommand("server /data")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin");

    @Autowired
    private MinioClient minioClient;

    @Test
    void minioClientShouldBeConfigured() throws Exception {
        assertNotNull(minioClient);

        String bucketName = "test-bucket";
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        assertTrue(minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()));
    }
}