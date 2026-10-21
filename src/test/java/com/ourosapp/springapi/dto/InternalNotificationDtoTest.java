package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationItemDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationRequestDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationResponseDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para os DTOs de notificação interna em lote:
 * {@link InternalBatchNotificationItemDTO}, {@link InternalBatchNotificationRequestDTO}
 * e {@link InternalBatchNotificationResponseDTO}.
 */
class InternalNotificationDtoTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve validar com sucesso um item de notificação completo e correto")
    void deveValidarItemCorreto() {
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "aud:high_water:1",
                10L,
                "FARM_OWNER",
                "Alerta de Água",
                "Consumo acima da média",
                "TIP_ACTIVATED",
                3L,
                "{\"farm_id\": 5}",
                true
        );

        Set<ConstraintViolation<InternalBatchNotificationItemDTO>> violations = validator.validate(item);
        assertTrue(violations.isEmpty(), "Não deve haver violações de validação");
        assertEquals("aud:high_water:1", item.eventId());
        assertEquals(10L, item.userId());
        assertEquals("FARM_OWNER", item.userRole());
        assertEquals("Alerta de Água", item.title());
        assertEquals("Consumo acima da média", item.message());
        assertEquals("TIP_ACTIVATED", item.type());
        assertEquals(3L, item.idTip());
        assertEquals("{\"farm_id\": 5}", item.dataPayload());
        assertTrue(item.sendPush());
    }

    @Test
    @DisplayName("Deve aplicar sanitizações e defaults no compact constructor de InternalBatchNotificationItemDTO")
    void deveAplicarSanitizacaoEDefaultsNoItem() {
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "   ",
                20L,
                "  company_employee  ",
                "  Título com Espaços  ",
                "  Mensagem com Espaços  ",
                "   ",
                null,
                "   ",
                null
        );

        assertNull(item.eventId(), "eventId em branco deve ser sanitizado para null");
        assertEquals("COMPANY_EMPLOYEE", item.userRole(), "userRole deve sofrer trim e conversão para maiúsculo");
        assertEquals("Título com Espaços", item.title());
        assertEquals("Mensagem com Espaços", item.message());
        assertEquals("TIP_ACTIVATED", item.type(), "type em branco deve assumir default TIP_ACTIVATED");
        assertNull(item.dataPayload(), "dataPayload em branco deve ser sanitizado para null");
        assertTrue(item.sendPush(), "sendPush nulo deve assumir default true");
    }

    @Test
    @DisplayName("Deve rejeitar item quando campos obrigatórios estiverem ausentes ou inválidos")
    void deveRejeitarCamposInvalidosNoItem() {
        InternalBatchNotificationItemDTO itemInvalido = new InternalBatchNotificationItemDTO(
                "a".repeat(256),
                null,
                "INVALID_ROLE",
                "",
                "",
                "CUSTOM_TYPE",
                null,
                null,
                false
        );

        Set<ConstraintViolation<InternalBatchNotificationItemDTO>> violations = validator.validate(itemInvalido);
        assertFalse(violations.isEmpty());

        List<String> propertyPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertTrue(propertyPaths.contains("eventId"));
        assertTrue(propertyPaths.contains("userId"));
        assertTrue(propertyPaths.contains("userRole"));
        assertTrue(propertyPaths.contains("title"));
        assertTrue(propertyPaths.contains("message"));
    }

    @Test
    @DisplayName("Deve rejeitar título com mais de 150 caracteres")
    void deveRejeitarTituloComMaisDe150Caracteres() {
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "evt-1",
                1L,
                "ADM",
                "T".repeat(151),
                "Mensagem válida",
                "SYSTEM",
                null,
                null,
                false
        );

        Set<ConstraintViolation<InternalBatchNotificationItemDTO>> violations = validator.validate(item);
        assertEquals(1, violations.size());
        assertEquals("title", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    @DisplayName("Deve validar requisição válida em lote com múltiplos itens")
    void deveValidarRequisicaoValida() {
        InternalBatchNotificationItemDTO item1 = new InternalBatchNotificationItemDTO(
                "evt-1", 10L, "FARM_OWNER", "Título 1", "Mensagem 1", "TIP_ACTIVATED", null, null, true
        );
        InternalBatchNotificationItemDTO item2 = new InternalBatchNotificationItemDTO(
                "evt-2", 20L, "COMPANY_EMPLOYEE", "Título 2", "Mensagem 2", "TIP_RESOLVED", 5L, null, false
        );

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item1, item2));
        Set<ConstraintViolation<InternalBatchNotificationRequestDTO>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
        assertEquals(2, request.notifications().size());
    }

    @Test
    @DisplayName("Deve rejeitar requisição em lote com lista vazia ou nula")
    void deveRejeitarRequisicaoVaziaOuNula() {
        InternalBatchNotificationRequestDTO requestVazia = new InternalBatchNotificationRequestDTO(List.of());
        Set<ConstraintViolation<InternalBatchNotificationRequestDTO>> violationsVazia = validator.validate(requestVazia);
        assertFalse(violationsVazia.isEmpty());

        InternalBatchNotificationRequestDTO requestNula = new InternalBatchNotificationRequestDTO(null);
        Set<ConstraintViolation<InternalBatchNotificationRequestDTO>> violationsNula = validator.validate(requestNula);
        assertFalse(violationsNula.isEmpty());
    }

    @Test
    @DisplayName("Deve rejeitar requisição que exceder o limite máximo de 500 itens")
    void deveRejeitarRequisicaoExcedendoLimiteMaximo() {
        List<InternalBatchNotificationItemDTO> items = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            items.add(new InternalBatchNotificationItemDTO(
                    "evt-" + i, (long) i, "FARM_OWNER", "Título", "Mensagem", "TIP_ACTIVATED", null, null, true
            ));
        }

        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(items);
        Set<ConstraintViolation<InternalBatchNotificationRequestDTO>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("notifications")));
    }

    @Test
    @DisplayName("Deve serializar e desserializar DTOs com suporte a snake_case e camelCase")
    void deveSerializarEDesserializarJson() throws Exception {
        String jsonInput = """
                {
                  "event_id": "aud:high_water:10",
                  "user_id": 15,
                  "user_role": "FARM_OWNER",
                  "title": "Alerta",
                  "message": "Consumo elevado",
                  "type": "TIP_ACTIVATED",
                  "id_tip": 99,
                  "data_payload": "{\\"metric\\": \\"water\\"}",
                  "send_push": false
                }
                """;

        InternalBatchNotificationItemDTO item = objectMapper.readValue(jsonInput, InternalBatchNotificationItemDTO.class);
        assertEquals("aud:high_water:10", item.eventId());
        assertEquals(15L, item.userId());
        assertEquals("FARM_OWNER", item.userRole());
        assertEquals("Alerta", item.title());
        assertEquals("Consumo elevado", item.message());
        assertEquals("TIP_ACTIVATED", item.type());
        assertEquals(99L, item.idTip());
        assertEquals("{\"metric\": \"water\"}", item.dataPayload());
        assertFalse(item.sendPush());

        // Teste de desserialização em camelCase
        String jsonCamelCase = """
                {
                  "eventId": "aud:high_water:20",
                  "userId": 25,
                  "userRole": "COMPANY_EMPLOYEE",
                  "title": "Alerta",
                  "message": "Consumo elevado",
                  "idTip": 101,
                  "dataPayload": "{}",
                  "sendPush": true
                }
                """;

        InternalBatchNotificationItemDTO itemCamel = objectMapper.readValue(jsonCamelCase, InternalBatchNotificationItemDTO.class);
        assertEquals("aud:high_water:20", itemCamel.eventId());
        assertEquals(25L, itemCamel.userId());
        assertEquals("COMPANY_EMPLOYEE", itemCamel.userRole());
        assertEquals(101L, itemCamel.idTip());
        assertTrue(itemCamel.sendPush());

        // Teste de serialização e desserialização de InternalBatchNotificationResponseDTO
        LocalDateTime now = LocalDateTime.of(2026, 10, 21, 11, 45, 0);
        InternalBatchNotificationResponseDTO response = new InternalBatchNotificationResponseDTO(10, 8, 2, 8, now);

        String jsonResponse = objectMapper.writeValueAsString(response);
        assertTrue(jsonResponse.contains("\"total_received\":10"));
        assertTrue(jsonResponse.contains("\"total_created\":8"));
        assertTrue(jsonResponse.contains("\"total_skipped\":2"));
        assertTrue(jsonResponse.contains("\"total_push_dispatched\":8"));
        assertTrue(jsonResponse.contains("\"processed_at\":\"2026-10-21T11:45:00\""));

        InternalBatchNotificationResponseDTO parsedResponse = objectMapper.readValue(jsonResponse, InternalBatchNotificationResponseDTO.class);
        assertEquals(response, parsedResponse);
    }
}
