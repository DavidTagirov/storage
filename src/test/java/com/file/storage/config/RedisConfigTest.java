package com.file.storage.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Tag("integration")
@Tag("redis")
class RedisConfigTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void redisConnectionFactoryShouldBeConfigured() {
        assertNotNull(redisConnectionFactory);

        try {
            var connection = redisConnectionFactory.getConnection();
            assertEquals("PONG", connection.ping());
            connection.close();
        } catch (Exception e) {
            fail("Redis connection failed", e);
        }
    }
}