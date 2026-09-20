package com.ourosapp.springapi.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationRateLimitFilterTest {

    @Test
    @DisplayName("Deve limitar cadastros públicos por IP e retornar 429")
    void shouldRateLimitPublicRegistrationsByIp() throws Exception {
        RegistrationRateLimitFilter filter = new RegistrationRateLimitFilter(2, 60);

        MockHttpServletResponse first = execute(filter, "POST", "/farm-owners", "10.0.0.1", null);
        MockHttpServletResponse second = execute(filter, "POST", "/company-employees", "10.0.0.1", null);
        MockHttpServletResponse third = execute(filter, "POST", "/farm-owners", "10.0.0.1", null);

        assertEquals(200, first.getStatus());
        assertEquals("1", first.getHeader("X-RateLimit-Remaining"));

        assertEquals(200, second.getStatus());
        assertEquals("0", second.getHeader("X-RateLimit-Remaining"));

        assertEquals(429, third.getStatus());
        assertEquals("2", third.getHeader("X-RateLimit-Limit"));
        assertEquals("0", third.getHeader("X-RateLimit-Remaining"));
        assertTrue(Long.parseLong(third.getHeader("Retry-After")) >= 1);
        assertTrue(third.getContentAsString().contains("Muitas tentativas de cadastro"));
    }

    @Test
    @DisplayName("Deve manter limites independentes para IPs diferentes")
    void shouldKeepSeparateBucketsPerIp() throws Exception {
        RegistrationRateLimitFilter filter = new RegistrationRateLimitFilter(1, 60);

        MockHttpServletResponse firstIp = execute(filter, "POST", "/farm-owners", "10.0.0.1", null);
        MockHttpServletResponse secondIp = execute(filter, "POST", "/farm-owners", "10.0.0.2", null);

        assertEquals(200, firstIp.getStatus());
        assertEquals(200, secondIp.getStatus());
    }

    @Test
    @DisplayName("Deve usar CF-Connecting-IP quando disponível")
    void shouldUseCloudflareConnectingIpWhenPresent() throws Exception {
        RegistrationRateLimitFilter filter = new RegistrationRateLimitFilter(1, 60);

        MockHttpServletResponse first = execute(filter, "POST", "/farm-owners", "172.16.0.1", "203.0.113.10");
        MockHttpServletResponse second = execute(filter, "POST", "/farm-owners", "172.16.0.1", "203.0.113.11");

        assertEquals(200, first.getStatus());
        assertEquals(200, second.getStatus());
    }

    @Test
    @DisplayName("Não deve aplicar rate limit a métodos ou rotas protegidas diferentes")
    void shouldIgnoreNonRegistrationRequests() throws Exception {
        RegistrationRateLimitFilter filter = new RegistrationRateLimitFilter(1, 60);

        MockHttpServletResponse getResponse = execute(filter, "GET", "/farm-owners", "10.0.0.1", null);
        MockHttpServletResponse otherPost = execute(filter, "POST", "/farms", "10.0.0.1", null);
        MockHttpServletResponse registration = execute(filter, "POST", "/farm-owners", "10.0.0.1", null);

        assertEquals(200, getResponse.getStatus());
        assertEquals(200, otherPost.getStatus());
        assertEquals(200, registration.getStatus());
        assertEquals("0", registration.getHeader("X-RateLimit-Remaining"));
    }

    private MockHttpServletResponse execute(
            RegistrationRateLimitFilter filter,
            String method,
            String path,
            String remoteAddr,
            String cloudflareIp
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddr);
        if (cloudflareIp != null) {
            request.addHeader("CF-Connecting-IP", cloudflareIp);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
