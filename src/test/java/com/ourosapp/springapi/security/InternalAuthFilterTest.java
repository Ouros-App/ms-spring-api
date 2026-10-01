package com.ourosapp.springapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalAuthFilterTest {

    private static final String SECRET = "super-secret-key-32-chars-long-123456";

    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("Deve permitir requisição e definir autenticação quando a chave for válida")
    void shouldAuthenticateWhenKeyIsValid() throws ServletException, IOException {
        InternalAuthFilter filter = new InternalAuthFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/password-reset");
        request.addHeader(InternalAuthFilter.INTERNAL_KEY_HEADER, SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Contexto deve ser limpo no finally");
    }

    @Test
    @DisplayName("Deve retornar 401 quando o cabeçalho estiver ausente")
    void shouldReturn401WhenHeaderIsMissing() throws ServletException, IOException {
        InternalAuthFilter filter = new InternalAuthFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/password-reset");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Acesso não autorizado"));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Deve retornar 401 quando o cabeçalho tiver chave incorreta")
    void shouldReturn401WhenKeyIsWrong() throws ServletException, IOException {
        InternalAuthFilter filter = new InternalAuthFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/password-reset");
        request.addHeader(InternalAuthFilter.INTERNAL_KEY_HEADER, "invalid-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Acesso não autorizado"));
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Deve retornar 401 quando a chave configurada na aplicação estiver vazia")
    void shouldReturn401WhenConfiguredKeyIsEmpty() throws ServletException, IOException {
        InternalAuthFilter filter = new InternalAuthFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/password-reset");
        request.addHeader(InternalAuthFilter.INTERNAL_KEY_HEADER, SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Deve ignorar requisições que não iniciam com /internal/v1/")
    void shouldNotFilterNonInternalPaths() {
        InternalAuthFilter filter = new InternalAuthFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/farm-owners");

        assertTrue(filter.shouldNotFilter(request));
    }
}
