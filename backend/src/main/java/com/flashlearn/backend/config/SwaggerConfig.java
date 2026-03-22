package com.flashlearn.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("FlashLearn API")
                .description("API do nauki przez fiszki")
                .version("1.0"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
            .components(new Components()
                .addSecuritySchemes("Bearer Token", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Wklej token JWT (bez 'Bearer ')")));
    }
}
