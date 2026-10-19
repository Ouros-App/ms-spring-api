package com.ourosapp.springapi.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade {@link Notification}.
 */
class NotificationEntityTest {

    @Test
    @DisplayName("Deve validar instanciação, getters, setters, builder e toString")
    void deveValidarGettersSettersEBuilder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime readAt = now.plusMinutes(5);

        Notification notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .userRole("FARM_OWNER")
                .title("Título Teste")
                .message("Mensagem Teste")
                .type("TIP_CREATED")
                .idTip(99L)
                .dataPayload("{\"key\":\"val\"}")
                .isRead(true)
                .createdAt(now)
                .readAt(readAt)
                .build();

        assertEquals(1L, notification.getId());
        assertEquals(10L, notification.getUserId());
        assertEquals("FARM_OWNER", notification.getUserRole());
        assertEquals("Título Teste", notification.getTitle());
        assertEquals("Mensagem Teste", notification.getMessage());
        assertEquals("TIP_CREATED", notification.getType());
        assertEquals(99L, notification.getIdTip());
        assertEquals("{\"key\":\"val\"}", notification.getDataPayload());
        assertTrue(notification.isRead());
        assertEquals(now, notification.getCreatedAt());
        assertEquals(readAt, notification.getReadAt());
        assertNotNull(notification.toString());

        Notification defaultNotification = new Notification();
        defaultNotification.setId(2L);
        defaultNotification.setUserId(20L);
        defaultNotification.setUserRole("ADM");
        defaultNotification.setTitle("Novo Título");
        defaultNotification.setMessage("Nova Mensagem");
        defaultNotification.setType("RANKING_UPDATED");
        defaultNotification.setIdTip(null);
        defaultNotification.setDataPayload(null);
        defaultNotification.setRead(false);
        defaultNotification.setCreatedAt(now);
        defaultNotification.setReadAt(null);

        assertEquals(2L, defaultNotification.getId());
        assertEquals(20L, defaultNotification.getUserId());
        assertEquals("ADM", defaultNotification.getUserRole());
        assertFalse(defaultNotification.isRead());
    }

    @Test
    @DisplayName("onCreate - Deve definir createdAt quando nulo e preservar quando já informado")
    void deveExecutarOnCreateCorretamente() {
        Notification notification = new Notification();
        assertNull(notification.getCreatedAt());

        notification.onCreate();
        assertNotNull(notification.getCreatedAt());

        LocalDateTime customDate = LocalDateTime.of(2026, 1, 1, 10, 0);
        notification.setCreatedAt(customDate);
        notification.onCreate();
        assertEquals(customDate, notification.getCreatedAt());
    }
}
