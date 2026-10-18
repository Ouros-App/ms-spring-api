package com.ourosapp.springapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link RequestLoggingFilter}.
 */
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Deve registrar log INFO para status 200 OK com query string")
    void deveRegistrarLogInfoParaStatus200() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/farms");
        request.setQueryString("page=0&size=10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve registrar log WARN para status 400 Bad Request sem query string")
    void deveRegistrarLogWarnParaStatus400() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/lots");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(400);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve registrar log ERROR para status 500 Internal Server Error")
    void deveRegistrarLogErrorParaStatus500() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/enterprises/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve relançar exceção e registrar log ERROR quando filterChain falhar")
    void deveRelancarExcecaoQuandoChainFalhar() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/enterprises");
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new RuntimeException("Simulated error")).when(filterChain).doFilter(request, response);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> filter.doFilter(request, response, filterChain)
        );

        assertEquals("Simulated error", exception.getMessage());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve ignorar rotas de observabilidade como health, metrics e favicon")
    void deveIgnorarRotasDeObservabilidade() {
        MockHttpServletRequest healthRequest = new MockHttpServletRequest("GET", "/health");
        assertTrue(filter.shouldNotFilter(healthRequest));

        MockHttpServletRequest metricsRequest = new MockHttpServletRequest("GET", "/metrics");
        assertTrue(filter.shouldNotFilter(metricsRequest));

        MockHttpServletRequest faviconRequest = new MockHttpServletRequest("GET", "/favicon.ico");
        assertTrue(filter.shouldNotFilter(faviconRequest));

        MockHttpServletRequest regularRequest = new MockHttpServletRequest("GET", "/farms");
        assertFalse(filter.shouldNotFilter(regularRequest));
    }
}
