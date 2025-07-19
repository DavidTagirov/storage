package com.file.storage.integration;

import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Tag("integration")
@Tag("minio")
class MinioFileStorageTest {

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio")
            .withCommand("server /data")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin");

    @Autowired
    private MinioClient minioClient;

    private final String bucketName = "test-bucket";
    private final String testFileName = "test-file.txt";
    private final String testContent = "Hello MinIO!";

    @BeforeEach
    void setUp() throws Exception {
        // Создаем бакет перед каждым тестом
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @Test
    void shouldUploadAndDownloadFile() throws Exception {
        // Загружаем файл
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(testFileName)
                        .stream(new ByteArrayInputStream(testContent.getBytes()), testContent.length(), -1)
                        .build());

        // Проверяем, что файл существует
        assertTrue(fileExistsInMinio());

        // Скачиваем файл
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(testFileName)
                        .build())) {

            String downloadedContent = new String(stream.readAllBytes());
            assertEquals(testContent, downloadedContent);
        }
    }

    @Test
    void shouldDeleteFile() throws Exception {
        // Сначала загружаем файл
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(testFileName)
                        .stream(new ByteArrayInputStream(testContent.getBytes()), testContent.length(), -1)
                        .build());

        // Удаляем файл
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(testFileName)
                        .build());

        // Проверяем, что файл удален
        assertFalse(fileExistsInMinio());
    }

    private boolean fileExistsInMinio() {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(testFileName)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}