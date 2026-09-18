package com.ourosapp.springapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuração do RestClient HTTP para comunicação com o microsserviço ms-auth-service.
 */
@Configuration
public class AuthClientConfig {

    @Bean
    public RestClient authServiceRestClient(
            @Value("${app.auth-service.base-url:http://localhost:8000}") String baseUrl,
            @Value("${app.auth-service.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${app.auth-service.read-timeout-seconds:5}") int readTimeoutSeconds
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
