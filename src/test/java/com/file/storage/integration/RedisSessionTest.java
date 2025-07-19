/*
package com.file.storage.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Tag("integration")
@Tag("redis")
class RedisSessionTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    @Autowired
    private SessionRepository<Session> sessionRepository; // Изменили тип на конкретный Session

    @Test
    void shouldSaveAndRetrieveSession() {
        // Создаем сессию
        Session session = sessionRepository.createSession();
        session.setAttribute("testKey", "testValue");

        // Сохраняем сессию
        sessionRepository.save(session);

        // Получаем сессию по ID
        Session retrieved = sessionRepository.findById(session.getId());

        // Проверяем
        assertNotNull(retrieved, "Сессия должна быть найдена");
        assertEquals("testValue", retrieved.getAttribute("testKey"),
                "Атрибут сессии должен совпадать");

        // Очищаем
        sessionRepository.deleteById(session.getId());
    }
}*/
