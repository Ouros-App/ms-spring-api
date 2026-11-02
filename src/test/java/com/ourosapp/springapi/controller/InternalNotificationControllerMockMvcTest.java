package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationItemDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationRequestDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationResponseDTO;
import com.ourosapp.springapi.security.InternalAuthFilter;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.service.InternalNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalNotificationController.class)
@Import({SecurityConfig.class, InternalAuthFilter.class})
@TestPropertySource(properties = {
        "app.internal.service-key=test-internal-secret-key-32-chars-long"
})
class InternalNotificationControllerMockMvcTest {

    private static final String INTERNAL_KEY = "test-internal-secret-key-32-chars-long";
    private static final String ENDPOINT = "/internal/v1/notifications/batch";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalNotificationService internalNotificationService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Deve processar o lote com sucesso quando chave interna e payload forem válidos (200 OK)")
    void shouldProcessBatchSuccessfullyWhenInternalKeyIsValid() throws Exception {
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "evt-123",
                10L,
                "FARM_OWNER",
                "Alerta de Consumo de Água",
                "Detectamos anomalia no consumo de água da sua granja.",
                "TIP_ACTIVATED",
                3L,
                "{\"farm_id\": 5}",
                true
        );
        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item));

        InternalBatchNotificationResponseDTO responseDTO = new InternalBatchNotificationResponseDTO(
                1, 1, 0, 1, LocalDateTime.of(2026, 10, 21, 11, 45, 0)
        );

        when(internalNotificationService.processBatch(any(InternalBatchNotificationRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_received").value(1))
                .andExpect(jsonPath("$.total_created").value(1))
                .andExpect(jsonPath("$.total_skipped").value(0))
                .andExpect(jsonPath("$.total_push_queued").value(1))
                .andExpect(jsonPath("$.processed_at").value("2026-10-21T11:45:00"));

        verify(internalNotificationService).processBatch(any(InternalBatchNotificationRequestDTO.class));
    }

    @Test
    @DisplayName("Deve rejeitar com 401 quando o cabeçalho X-Internal-Service-Key estiver ausente")
    void shouldReturnUnauthorizedWhenInternalKeyIsMissing() throws Exception {
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "evt-123", 10L, "FARM_OWNER", "Título", "Mensagem", "TIP_ACTIVATED", null, null, true
        );
        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Acesso não autorizado. Chave interna ausente ou inválida."));

        verifyNoInteractions(internalNotificationService);
    }

    @Test
    @DisplayName("Deve rejeitar com 401 quando o cabeçalho X-Internal-Service-Key for inválido")
    void shouldReturnUnauthorizedWhenInternalKeyIsInvalid() throws Exception {
        InternalBatchNotificationItemDTO item = new InternalBatchNotificationItemDTO(
                "evt-123", 10L, "FARM_OWNER", "Título", "Mensagem", "TIP_ACTIVATED", null, null, true
        );
        InternalBatchNotificationRequestDTO request = new InternalBatchNotificationRequestDTO(List.of(item));

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Acesso não autorizado. Chave interna ausente ou inválida."));

        verifyNoInteractions(internalNotificationService);
    }

    @Test
    @DisplayName("Deve rejeitar com 400 Bad Request quando a lista de notificações for vazia")
    void shouldReturnBadRequestWhenNotificationsListIsEmpty() throws Exception {
        String payload = """
                {
                  "notifications": []
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(internalNotificationService);
    }

    @Test
    @DisplayName("Deve rejeitar com 400 Bad Request quando a lista de notificações for nula ou ausente")
    void shouldReturnBadRequestWhenNotificationsFieldIsMissing() throws Exception {
        String payload = "{}";

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(internalNotificationService);
    }

    @Test
    @DisplayName("Deve rejeitar com 400 Bad Request quando item contiver campos obrigatórios ausentes ou inválidos")
    void shouldReturnBadRequestWhenItemHasMissingMandatoryFields() throws Exception {
        String payload = """
                {
                  "notifications": [
                    {
                      "event_id": "evt-1",
                      "user_id": null,
                      "user_role": "INVALID_ROLE",
                      "title": "",
                      "message": ""
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(internalNotificationService);
    }
}
