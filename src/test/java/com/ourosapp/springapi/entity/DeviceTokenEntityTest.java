package com.ourosapp.springapi.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade {@link DeviceToken}.
 */
class DeviceTokenEntityTest {

    @Test
    @DisplayName("Deve validar getters, setters, builder e toString")
    void deveValidarGettersSettersEBuilder() {
        LocalDateTime now = LocalDateTime.now();

        DeviceToken token = DeviceToken.builder()
                .id(1L)
                .userId(10L)
                .userRole("FARM_OWNER")
                .token("fcm_token_123")
                .platform("ANDROID")
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, token.getId());
        assertEquals(10L, token.getUserId());
        assertEquals("FARM_OWNER", token.getUserRole());
        assertEquals("fcm_token_123", token.getToken());
        assertEquals("ANDROID", token.getPlatform());
        assertEquals(now, token.getCreatedAt());
        assertEquals(now, token.getUpdatedAt());
        assertNotNull(token.toString());

        DeviceToken emptyToken = new DeviceToken();
        emptyToken.setId(2L);
        emptyToken.setUserId(20L);
        emptyToken.setUserRole("COMPANY_EMPLOYEE");
        emptyToken.setToken("fcm_token_456");
        emptyToken.setPlatform("IOS");
        emptyToken.setCreatedAt(now);
        emptyToken.setUpdatedAt(now);

        assertEquals(2L, emptyToken.getId());
        assertEquals("fcm_token_456", emptyToken.getToken());
        assertEquals("IOS", emptyToken.getPlatform());
    }

    @Test
    @DisplayName("onCreate e onUpdate - Deve inicializar e atualizar timestamps")
    void deveExecutarCicloDeVidaDeTimestamps() {
        DeviceToken token = new DeviceToken();
        assertNull(token.getCreatedAt());
        assertNull(token.getUpdatedAt());

        token.onCreate();
        assertNotNull(token.getCreatedAt());
        assertNotNull(token.getUpdatedAt());

        LocalDateTime past = LocalDateTime.of(2026, 1, 1, 12, 0);
        token.setCreatedAt(past);
        token.setUpdatedAt(past);
        token.onCreate();
        assertEquals(past, token.getCreatedAt());
        assertEquals(past, token.getUpdatedAt());

        token.onUpdate();
        assertNotNull(token.getUpdatedAt());
        assertNotEquals(past, token.getUpdatedAt());
    }
}
