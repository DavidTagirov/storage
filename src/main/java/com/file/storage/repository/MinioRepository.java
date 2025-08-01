package com.file.storage.repository;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.ResourceAccessException;

import java.io.InputStream;

@Repository
public class MinioRepository {
    private final MinioClient minioClient;
    private final String bucketName;

    public MinioRepository(MinioClient minioClient,
                           @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public Iterable<Result<Item>> listObjects(String prefix, String delimiter, Boolean recursive) {
        ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix);

        if (delimiter != null) {
            builder.delimiter(delimiter);
        }
        if (recursive != null) {
            builder.recursive(recursive);
        }

        return minioClient.listObjects(builder.build());
    }

    public InputStream getObject(String object) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(object)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void copyObject(String objectFrom, String objectTo) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectTo)
                    .source(CopySource.builder()
                            .bucket(bucketName)
                            .object(objectFrom)
                            .build())
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removeObject(String object) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(object)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public StatObjectResponse statObject(String object) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(object)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putObject(String object, InputStream inputStream, long objectSize, int partSize) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(object)
                    .stream(inputStream, objectSize, partSize)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Не удалось проверить или создать бакет", e);
        }
    }

    public boolean resourceOrDirectoryExists(String fullPath) {
        try {
            if (fullPath.endsWith("/")) {
                boolean hasObjectsFolder = minioClient.listObjects(ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .maxKeys(1)
                        .build()
                ).iterator().hasNext();

                boolean isEmptyFolder = minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build()
                ) != null;
                return hasObjectsFolder || isEmptyFolder;
            } else {
                minioClient.statObject(StatObjectArgs.builder()
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
            throw new ResourceAccessException("Ошибка проверки существования ресурса");
        } catch (Exception e) {
            throw new ResourceAccessException("Failed to check resource existence");
        }
    }
}