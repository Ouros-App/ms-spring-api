package com.ourosapp.springapi.client.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.client.auth.dto.AuthVerifyResponseDTO;
import com.ourosapp.springapi.client.auth.exception.AuthRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AuthServiceClientTest {

    private MockRestServiceServer mockServer;
    private AuthServiceClient authServiceClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        authServiceClient = new AuthServiceClient(restClient);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testVerifyCredentialsSuccess() throws Exception {
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                42L, "user@example.com", "farm_owner", "farm_owner", "Fazendeiro João", 7L, null, false
        );
        AuthVerifyResponseDTO responseDTO = new AuthVerifyResponseDTO(true, identity);

        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseDTO), MediaType.APPLICATION_JSON));

        Optional<AuthVerifyResponseDTO.IdentityDTO> result = authServiceClient
                .verifyCredentials("user@example.com", "senha123", "farm_owner");

        mockServer.verify();
        assertTrue(result.isPresent());
        assertEquals(42L, result.get().id());
        assertEquals("user@example.com", result.get().email());
        assertEquals("farm_owner", result.get().accountType());
        assertEquals(7L, result.get().farmId());
        assertFalse(result.get().firstAccess());
    }

    @Test
    void testVerifyCredentialsUnauthorizedReturnsEmpty() {
        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        Optional<AuthVerifyResponseDTO.IdentityDTO> result = authServiceClient
                .verifyCredentials("user@example.com", "errada", "admin");

        mockServer.verify();
        assertTrue(result.isEmpty());
    }

    @Test
    void testVerifyCredentialsRateLimitThrowsAuthRateLimitExceptionWithRetryAfter() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "30");

        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withRawStatus(HttpStatus.TOO_MANY_REQUESTS.value())
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Muitas tentativas.\"}"));

        AuthRateLimitException ex = assertThrows(AuthRateLimitException.class, () ->
                authServiceClient.verifyCredentials("user@example.com", "senha", "admin")
        );

        mockServer.verify();
        assertEquals(30L, ex.getRetryAfterSeconds());
        assertTrue(ex.getMessage().contains("Muitas tentativas"));
    }

    @Test
    void testVerifyCredentialsRateLimitWithoutRetryAfterHeader() {
        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withRawStatus(HttpStatus.TOO_MANY_REQUESTS.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Muitas tentativas.\"}"));

        AuthRateLimitException ex = assertThrows(AuthRateLimitException.class, () ->
                authServiceClient.verifyCredentials("user@example.com", "senha", "admin")
        );

        mockServer.verify();
        assertNull(ex.getRetryAfterSeconds());
    }

    @Test
    void testVerifyCredentialsConflictThrowsResponseStatusException() {
        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withRawStatus(HttpStatus.CONFLICT.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Ambiguous credentials.\"}"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authServiceClient.verifyCredentials("user@example.com", "senha", null)
        );

        mockServer.verify();
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("As credenciais correspondem a mais de uma conta"));
    }

    @Test
    void testVerifyCredentialsServerErrorThrows503() {
        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authServiceClient.verifyCredentials("user@example.com", "senha", "company_employee")
        );

        mockServer.verify();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals("Serviço de autenticação temporariamente indisponível.", ex.getReason());
    }

    @Test
    void testVerifyCredentialsClientErrorBadRequestThrowsResponseStatusException() {
        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authServiceClient.verifyCredentials("user@example.com", "senha", "admin")
        );

        mockServer.verify();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testVerifyCredentialsRateLimitWithNonNumericRetryAfter() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "not-a-number");

        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withRawStatus(HttpStatus.TOO_MANY_REQUESTS.value())
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"Rate limited\"}"));

        AuthRateLimitException ex = assertThrows(AuthRateLimitException.class, () ->
                authServiceClient.verifyCredentials("user@example.com", "senha", "admin")
        );

        mockServer.verify();
        assertNull(ex.getRetryAfterSeconds());
    }

    @Test
    void testVerifyCredentialsUnauthenticatedResponseReturnsEmpty() throws Exception {
        AuthVerifyResponseDTO responseDTO = new AuthVerifyResponseDTO(false, null);

        mockServer.expect(requestTo("http://localhost:8000/v1/auth/credentials/verify"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseDTO), MediaType.APPLICATION_JSON));

        Optional<AuthVerifyResponseDTO.IdentityDTO> result = authServiceClient
                .verifyCredentials("user@example.com", "senha", "admin");

        mockServer.verify();
        assertTrue(result.isEmpty());
    }
}
