package com.ourosapp.springapi.config;

import com.ourosapp.springapi.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração central de segurança do Spring Security.
 * Define filtros stateless, políticas de CORS, codificador de senhas e tratamento de exceções de autenticação.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    /**
     * Configura a cadeia de filtros de segurança HTTP.
     *
     * @param http o objeto HttpSecurity
     * @return a cadeia de filtros compilada
     * @throws Exception se ocorrer erro de configuração
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Desativa CSRF pois a API é 100% Stateless (autenticação via Authorization: Bearer JWT sem cookies de sessão)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/adms/login",
                                "/company-employees/login",
                                "/farm-owners/login"
                        ).permitAll()
                        .requestMatchers(
                                "/health",
                                "/",
                                "/error",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Acesso não autorizado. Token ausente ou inválido.")
                        )
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
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
