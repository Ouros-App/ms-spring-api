package com.ourosapp.springapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Filtro de segurança dedicado a autenticar chamadas Machine-to-Machine para endpoints internos (/internal/v1/**).
 * Valida a chave secreta compartilhada fornecida via cabeçalho {@code X-Internal-Service-Key} utilizando
 * comparação segura em tempo constante contra ataques de temporização (timing attacks).
 */
@Slf4j
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
    public static final String INTERNAL_PATH_PREFIX = "/internal/v1/";

    private final String internalServiceKey;

    public InternalAuthFilter(
            @Value("${app.internal.service-key:}") String internalServiceKey
    ) {
        this.internalServiceKey = internalServiceKey != null ? internalServiceKey.trim() : "";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

        if (internalServiceKey.isEmpty() || providedKey == null || !isValidKey(providedKey)) {
            log.warn("Tentativa de acesso não autorizado à rota interna: {}", request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"type":"about:blank","title":"Unauthorized","status":401,"detail":"Acesso não autorizado. Chave interna ausente ou inválida."}
                    """.trim());
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isValidKey(String providedKey) {
        byte[] expectedBytes = internalServiceKey.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = providedKey.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }
}
