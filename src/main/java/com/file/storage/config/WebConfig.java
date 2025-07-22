package com.file.storage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/")
                .setViewName("forward:/index.html");
    }

    /**
     * Глобальная конфигурация CORS для всего приложения.
     * Это необходимо для корректной работы с фронтендом, который запущен на другом порту,
     * и решает проблемы с передачей cookie между доменами.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Применяем ко всем путям внутри /api
                // Разрешаем запросы от стандартных портов для фронтенда.
                // Если ваш фронтенд работает на другом порту, добавьте его сюда.
                .allowedOrigins("http://localhost:3000", "http://localhost:8081", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // Разрешенные методы
                .allowedHeaders("*") // Разрешаем все заголовки
                .allowCredentials(true) // <-- КРИТИЧЕСКИ ВАЖНО для работы с cookie и сессиями
                .maxAge(3600);
    }
}
