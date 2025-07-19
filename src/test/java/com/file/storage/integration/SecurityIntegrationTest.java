package com.file.storage.integration;

import com.file.storage.dto.SignInRequest;
import com.file.storage.dto.SignUpRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
@Tag("security")
class SecurityIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRegisterAndAuthenticateUser() {
        // Регистрация
        SignUpRequest signUpRequest = new SignUpRequest("testuser", "password123");
        ResponseEntity<Void> signUpResponse = restTemplate.postForEntity(
                "/api/auth/sign-up",
                signUpRequest,
                Void.class);

        assertEquals(HttpStatus.CREATED, signUpResponse.getStatusCode());

        // Аутентификация
        SignInRequest signInRequest = new SignInRequest("testuser", "password123");
        ResponseEntity<Void> signInResponse = restTemplate.postForEntity(
                "/api/auth/sign-in",
                signInRequest,
                Void.class);

        assertEquals(HttpStatus.OK, signInResponse.getStatusCode());
        assertNotNull(signInResponse.getHeaders().get("Set-Cookie"));
    }

    @Test
    void shouldRejectUnauthorizedAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/user/me",
                String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}