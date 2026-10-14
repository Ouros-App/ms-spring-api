package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ourosapp.springapi.dto.notification.NotificationResponseDTO;
import com.ourosapp.springapi.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para {@link NotificationResponseDTO} e mapeamento da entidade {@link Notification}.
 */
class NotificationDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve instanciar NotificationResponseDTO com id_tip via fromEntity")
    void deveInstanciarComIdTipViaFromEntity() {
        LocalDateTime now = LocalDateTime.now();
        Notification entity = Notification.builder()
                .id(1L)
                .userId(10L)
                .userRole("FARM_OWNER")
                .title("Nova Dica Técnica!")
                .message("Confira a recomendação de ambiência.")
                .type("TIP_CREATED")
                .idTip(42L)
                .dataPayload("{\"tip_id\":42}")
                .isRead(false)
                .createdAt(now)
                .readAt(null)
                .build();

        NotificationResponseDTO dto = NotificationResponseDTO.fromEntity(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("Nova Dica Técnica!", dto.title());
        assertEquals("Confira a recomendação de ambiência.", dto.message());
        assertEquals("TIP_CREATED", dto.type());
        assertEquals(42L, dto.idTip());
        assertEquals("{\"tip_id\":42}", dto.dataPayload());
        assertFalse(dto.isRead());
        assertEquals(now, dto.createdAt());
        assertNull(dto.readAt());
    }

    @Test
    @DisplayName("Deve instanciar NotificationResponseDTO sem id_tip via fromEntity para evento de ranking")
    void deveInstanciarSemIdTipParaRanking() {
        LocalDateTime now = LocalDateTime.now();
        Notification entity = Notification.builder()
                .id(2L)
                .userId(10L)
                .userRole("FARM_OWNER")
                .title("Ranking Atualizado!")
                .message("Sua fazenda alcançou o 1º lugar.")
                .type("RANKING_UPDATED")
                .idTip(null)
                .dataPayload("{\"rank_position\":1}")
                .isRead(true)
                .createdAt(now.minusHours(1))
                .readAt(now)
                .build();

        NotificationResponseDTO dto = NotificationResponseDTO.fromEntity(entity);

        assertNotNull(dto);
        assertEquals(2L, dto.id());
        assertNull(dto.idTip());
        assertTrue(dto.isRead());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando Notification for nula no fromEntity")
    void deveLancarExcecaoQuandoNotificationNula() {
        assertThrows(NullPointerException.class, () -> NotificationResponseDTO.fromEntity(null));
    }

    @Test
    @DisplayName("Deve serializar e desserializar com snake_case id_tip no JSON")
    void deveSerializarEDesserializarComSnakeCase() throws Exception {
        NotificationResponseDTO original = new NotificationResponseDTO(
                1L,
                "Dica",
                "Mensagem",
                "TIP_CREATED",
                100L,
                "{}",
                false,
                LocalDateTime.of(2026, 10, 14, 12, 0, 0),
                null
        );

        String json = objectMapper.writeValueAsString(original);
        assertTrue(json.contains("\"id_tip\":100"));
        assertTrue(json.contains("\"data_payload\":"));
        assertTrue(json.contains("\"is_read\":false"));

        NotificationResponseDTO parsed = objectMapper.readValue(json, NotificationResponseDTO.class);
        assertEquals(100L, parsed.idTip());
        assertEquals("TIP_CREATED", parsed.type());
    }

    @Test
    @DisplayName("Deve desserializar com JsonAlias em camelCase idTip")
    void deveDesserializarComCamelCaseAlias() throws Exception {
        String json = """
                {
                    "id": 1,
                    "title": "Dica",
                    "message": "Mensagem",
                    "type": "TIP_CREATED",
                    "idTip": 55,
                    "dataPayload": "{}",
                    "isRead": true,
                    "createdAt": "2026-10-14T12:00:00",
                    "readAt": "2026-10-14T12:05:00"
                }
                """;

        NotificationResponseDTO parsed = objectMapper.readValue(json, NotificationResponseDTO.class);
        assertNotNull(parsed);
        assertEquals(55L, parsed.idTip());
        assertTrue(parsed.isRead());
    }

    @Test
    @DisplayName("Deve funcionar com construtor de conveniência de 8 parâmetros")
    void deveFuncionarComConstrutorDeConveniencia() {
        LocalDateTime now = LocalDateTime.now();
        NotificationResponseDTO dto = new NotificationResponseDTO(
                1L,
                "Título",
                "Mensagem",
                "RANKING_UPDATED",
                "{}",
                false,
                now,
                null
        );

        assertEquals(1L, dto.id());
        assertNull(dto.idTip());
        assertEquals("RANKING_UPDATED", dto.type());
    }
}
