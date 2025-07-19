/*
package com.file.storage.service;

import io.minio.*;
import com.file.storage.dto.ResourceInfoResponse;
import com.file.storage.dto.ResourceType;
import com.file.storage.exceptions.ResourceNotFoundException;
import com.file.storage.model.User;
import com.file.storage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Container
    public GenericContainer<?> minioContainer = new GenericContainer<>("minio/minio")
            .withCommand("server /data")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin");

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResourceService resourceService;

    private MinioClient minioClient;
    private final String bucketName = "test-bucket";
    private final String username = "testuser";
    private final Long userId = 1L;

    @BeforeEach
    void setUp() throws Exception {
        minioClient = MinioClient.builder()
                .endpoint("http://" + minioContainer.getHost() + ":" + minioContainer.getFirstMappedPort())
                .credentials("minioadmin", "minioadmin")
                .build();

        // Создаем бакет для тестов
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        resourceService = new ResourceService(minioClient, userRepository);

        // Мокируем пользователя
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    }

    @Test
    void getResourceInfo_ShouldReturnFileInfo() throws Exception {
        // Подготовка тестового файла в MinIO
        String objectName = "user-1-files/test.txt";
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(new ByteArrayInputStream("test content".getBytes()), -1, 10485760)
                .build());

        ResourceInfoResponse response = resourceService.getResourceInfo(username, "test.txt");

        assertEquals("test.txt", response.name());
        assertEquals(ResourceType.FILE, response.type());
        assertNotNull(response.size());
    }

    @Test
    void uploadResource_ShouldUploadFile() throws Exception {
        // Здесь нужно использовать MockMultipartFile для имитации загрузки файла
        // Тест будет аналогичен предыдущим, но с проверкой загрузки

        // Этот тест требует более сложной настройки Mock для MultipartFile
    }

    @Test
    void deleteResource_ShouldDeleteFile() throws Exception {
        String objectName = "user-1-files/to-delete.txt";
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(new ByteArrayInputStream("content".getBytes()), -1, 10485760)
                .build());

        resourceService.deleteResource(username, "to-delete.txt");

        assertThrows(ResourceNotFoundException.class, () ->
                minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()));
    }
}*/
