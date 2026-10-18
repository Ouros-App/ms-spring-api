package com.ourosapp.springapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de observabilidade HTTP responsável por registrar métricas de acesso
 * e diagnóstico de rotas (método, URI, status da resposta e tempo de execução).
 * Executa com a mais alta precedência para capturar todas as requisições que chegam à API.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = (request.getQueryString() != null && !request.getQueryString().isBlank())
                ? "?" + request.getQueryString()
                : "";

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[HTTP] {} {}{} - Falha inesperada durante a requisição após {} ms: {}",
                    method, uri, queryString, duration, ex.getMessage(), ex);
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (status >= 500) {
                log.error("[HTTP] {} {}{} - Status: {} ({} ms)", method, uri, queryString, status, duration);
            } else if (status >= 400) {
                log.warn("[HTTP] {} {}{} - Status: {} ({} ms)", method, uri, queryString, status, duration);
            } else {
                log.info("[HTTP] {} {}{} - Status: {} ({} ms)", method, uri, queryString, status, duration);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/health".equals(path)
                || "/metrics".equals(path)
                || path.endsWith("/favicon.ico");
    }
}
