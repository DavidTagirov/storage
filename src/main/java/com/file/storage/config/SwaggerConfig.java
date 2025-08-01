package com.file.storage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("File Storage API")
                        .version("1.0")
                        .description("API for file management system")
                        .contact(new Contact()
                                .name("Support")
                                .email("david.tagirov.03@gmail.com"))
                        .license(new License()
                                .name("Super-duper license 17.0")
                                .url("https://springdoc.org")));
    }
}