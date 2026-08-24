package com.ourosapp.springapi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "chave-de-teste-muito-longa-com-mais-de-256-bits-para-o-hmac-sha-256";
    private final long expirationMs = 3600000; // 1h

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", expirationMs);
        jwtUtil.validateAndInitSecret();
    }

    @Test
    void testGenerateAndExtractToken() {
        String token = jwtUtil.generateToken(1L, "user@test.com", "ADM");

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertNotNull(jwtUtil.extractAllClaims(token));
        assertEquals(1L, jwtUtil.getIdFromToken(token));
        assertEquals("user@test.com", jwtUtil.getEmailFromToken(token));
        assertEquals("ADM", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void testGenerateTokenWithoutId() {
        String token = jwtUtil.generateToken("employee@test.com", "COMPANY_EMPLOYEE");

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertNull(jwtUtil.getIdFromToken(token));
        assertEquals("employee@test.com", jwtUtil.getEmailFromToken(token));
        assertEquals("COMPANY_EMPLOYEE", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void testValidateTokenInvalid() {
        assertFalse(jwtUtil.validateToken("token-invalido"));
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void testValidateTokenExpired() {
        JwtUtil expiredJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(expiredJwtUtil, "secret", secret);
        ReflectionTestUtils.setField(expiredJwtUtil, "expirationMs", -1000L);
        expiredJwtUtil.validateAndInitSecret();

        String expiredToken = expiredJwtUtil.generateToken(2L, "expired@test.com", "FARM_OWNER");
        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void testValidateSecretValid() {
        assertDoesNotThrow(() -> jwtUtil.validateAndInitSecret());
    }

    @Test
    void testValidateSecretInvalid() {
        JwtUtil invalidJwt = new JwtUtil();
        ReflectionTestUtils.setField(invalidJwt, "secret", "curta");
        assertThrows(IllegalStateException.class, invalidJwt::validateAndInitSecret);

        ReflectionTestUtils.setField(invalidJwt, "secret", null);
        assertThrows(IllegalStateException.class, invalidJwt::validateAndInitSecret);
    }
}
