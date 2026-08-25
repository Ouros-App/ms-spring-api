package com.ourosapp.springapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração global do OpenAPI / Swagger para documentação e testes da API.
 * Configura metadados da aplicação e define o esquema de autenticação via Bearer JWT.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ms-spring-api",
                version = "0.0.1-SNAPSHOT",
                description = "API REST com Spring Boot para o ecossistema Ouros App.",
                contact = @Contact(
                        name = "Ouros App",
                        email = "ouros.app@gmail.com",
                        url = "https://github.com/Ouros-App"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://github.com/Ouros-App/ms-spring-api/blob/main/LICENSE"
                )
        )
)
@SecurityScheme(
        name = "BearerAuth",
        description = "Insira o token JWT gerado no endpoint de login.",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}

