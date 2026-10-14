package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.notification.DeviceTokenRequestDTO;
import com.ourosapp.springapi.dto.notification.NotificationResponseDTO;
import com.ourosapp.springapi.dto.notification.UnreadCountResponseDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.DeviceTokenService;
import com.ourosapp.springapi.service.NotificationService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link NotificationController}.
 */
@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class})
class NotificationControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private DeviceTokenService deviceTokenService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private UserPrincipal createMockUser(String role) {
        return new UserPrincipal(
                1L,
                "usuario@teste.com",
                "Usuario Teste",
                role,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @Test
    @DisplayName("Deve registrar token de dispositivo com 200 OK quando payload for válido")
    void deveRegistrarTokenComSucesso() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);
        DeviceTokenRequestDTO dto = new DeviceTokenRequestDTO("fcm_token_valid_123", "ANDROID");

        mockMvc.perform(post("/notifications/device-token")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token de dispositivo registrado com sucesso"));

        verify(deviceTokenService).registerOrUpdateToken(any(UserPrincipal.class), eq(dto));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request ao tentar registrar token com plataforma inválida")
    void deveRetornar400QuandoPlataformaInvalida() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);
        String invalidJson = """
                {
                    "token": "valid_token",
                    "platform": "WINDOWS_PHONE"
                }
                """;

        mockMvc.perform(post("/notifications/device-token")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando tentar registrar token sem autenticação")
    void deveRetornar401AoRegistrarTokenSemAutenticacao() throws Exception {
        DeviceTokenRequestDTO dto = new DeviceTokenRequestDTO("fcm_token_valid_123", "ANDROID");

        mockMvc.perform(post("/notifications/device-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve remover token de dispositivo com 204 No Content")
    void deveRemoverTokenComSucesso() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);

        mockMvc.perform(delete("/notifications/device-token/{token}", "fcm_token_sample")
                        .with(user(principal)))
                .andExpect(status().isNoContent());

        verify(deviceTokenService).deleteToken(any(UserPrincipal.class), eq("fcm_token_sample"));
    }

    @Test
    @DisplayName("Deve listar notificações do usuário com 200 OK")
    void deveListarNotificacoesComSucesso() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);
        NotificationResponseDTO dto = new NotificationResponseDTO(
                1L,
                "Ranking Atualizado!",
                "Sua fazenda ficou em 1º lugar.",
                "RANKING_UPDATED",
                "{\"farm_id\":1}",
                false,
                LocalDateTime.now(),
                null
        );

        when(notificationService.getUserNotifications(any(UserPrincipal.class), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/notifications")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Ranking Atualizado!"))
                .andExpect(jsonPath("$.content[0].is_read").value(false));
    }

    @Test
    @DisplayName("Deve retornar id_tip serializado quando notificação for de dica técnica")
    void deveRetornarIdTipSerializadoParaNotificacaoDeDica() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);
        NotificationResponseDTO dto = new NotificationResponseDTO(
                10L,
                "Nova Dica Técnica!",
                "Confira a nova recomendação técnica.",
                "TIP_CREATED",
                88L,
                "{\"tip_id\":88}",
                false,
                LocalDateTime.now(),
                null
        );

        when(notificationService.getUserNotifications(any(UserPrincipal.class), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/notifications")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].type").value("TIP_CREATED"))
                .andExpect(jsonPath("$.content[0].id_tip").value(88));
    }

    @Test
    @DisplayName("Deve retornar contagem de notificações não lidas com 200 OK")
    void deveRetornarContagemDeNaoLidasComSucesso() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);
        when(notificationService.getUnreadCount(any(UserPrincipal.class)))
                .thenReturn(new UnreadCountResponseDTO(4L));

        mockMvc.perform(get("/notifications/unread-count")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread_count").value(4));
    }

    @Test
    @DisplayName("Deve marcar notificação individual como lida com 200 OK")
    void deveMarcarNotificacaoComoLidaComSucesso() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);
        NotificationResponseDTO dto = new NotificationResponseDTO(
                1L,
                "Ranking Atualizado!",
                "Sua fazenda ficou em 1º lugar.",
                "RANKING_UPDATED",
                "{\"farm_id\":1}",
                true,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now()
        );

        when(notificationService.markAsRead(eq(1L), any(UserPrincipal.class)))
                .thenReturn(dto);

        mockMvc.perform(patch("/notifications/{id}/read", 1L)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.is_read").value(true));
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar marcar notificação inexistente como lida")
    void deveRetornar404AoMarcarNotificacaoInexistente() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);
        when(notificationService.markAsRead(eq(99L), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"));

        mockMvc.perform(patch("/notifications/{id}/read", 99L)
                        .with(user(principal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve marcar todas as notificações como lidas com 204 No Content")
    void deveMarcarTodasComoLidasComSucesso() throws Exception {
        UserPrincipal principal = createMockUser(RoleConstants.FARM_OWNER);

        mockMvc.perform(patch("/notifications/read-all")
                        .with(user(principal)))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(any(UserPrincipal.class));
    }
}
