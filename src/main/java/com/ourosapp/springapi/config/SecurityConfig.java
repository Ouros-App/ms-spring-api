package com.ourosapp.springapi.config;

import com.ourosapp.springapi.security.AudienceValidator;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.RegistrationRateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração central de segurança do Spring Security.
 * Configura o OAuth2 Resource Server para validação assíncrona de tokens JWT via JWKS do Keycloak.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Value("${app.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:https://ouros-keycloak.discloud.app/realms/ouros/protocol/openid-connect/certs}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://ouros-keycloak.discloud.app/realms/ouros}")
    private String issuerUri;

    @Value("${app.security.oauth2.audience:ms-spring-api}")
    private String requiredAudience;

    /**
     * Configura a cadeia de filtros de segurança HTTP.
     *
     * @param http o objeto HttpSecurity
     * @return a cadeia de filtros compilada
     * @throws Exception se ocorrer erro de configuração
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RegistrationRateLimitFilter registrationRateLimitFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/farm-owners", "/company-employees").permitAll()
                        .requestMatchers(
                                "/health",
                                "/",
                                "/error",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(registrationRateLimitFilter, BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Acesso não autorizado. Token ausente ou inválido.")
                        )
                )
                .build();
    }

    /**
     * Limita tentativas de cadastro público por endereço IP.
     * Os valores podem ser ajustados sem recompilar a aplicação.
     */
    @Bean
    public RegistrationRateLimitFilter registrationRateLimitFilter(
            @Value("${app.security.registration-rate-limit.max-requests:60}") int maxRequests,
            @Value("${app.security.registration-rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${app.security.registration-rate-limit.trust-proxy-headers:false}") boolean trustProxyHeaders
    ) {
        return new RegistrationRateLimitFilter(maxRequests, windowSeconds, trustProxyHeaders);
    }

    /**
     * Decoder de JWT configurado com validação de assinatura via JWKS, timestamps, emissor e audience.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(requiredAudience);
        OAuth2TokenValidator<Jwt> delegatingValidator = new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator);

        jwtDecoder.setJwtValidator(delegatingValidator);
        return jwtDecoder;
    }

    /**
     * Configura a origem, métodos e cabeçalhos permitidos para CORS.
     *
     * @return a fonte de configuração de CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = (allowedOrigins != null && !allowedOrigins.isEmpty())
                ? allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList()
                : List.of();

        if (!origins.isEmpty()) {
            config.setAllowedOriginPatterns(origins);
        } else {
            config.setAllowedOrigins(List.of());
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Bean de codificação de senhas utilizando o algoritmo BCrypt.
     *
     * @return PasswordEncoder instanciado com BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
