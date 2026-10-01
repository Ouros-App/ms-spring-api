package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.internal.PasswordResetInternalDTO;
import com.ourosapp.springapi.security.InternalAuthFilter;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.service.InternalAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalAuthController.class)
@Import({SecurityConfig.class, InternalAuthFilter.class})
@TestPropertySource(properties = {
        "app.internal.service-key=test-internal-secret-key-32-chars-long"
})
class InternalAuthControllerMockMvcTest {

    private static final String INTERNAL_KEY = "test-internal-secret-key-32-chars-long";
    private static final String ENDPOINT = "/internal/v1/password-reset";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalAuthService internalAuthService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Deve redefinir a senha com sucesso quando chave interna e payload forem válidos (200 OK)")
    void shouldResetPasswordSuccessfullyWhenInternalKeyIsValid() throws Exception {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("farm_owner", 10L, "NovaSenha@123");
        doNothing().when(internalAuthService).resetPassword(any(PasswordResetInternalDTO.class));

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha atualizada com sucesso."));

        verify(internalAuthService).resetPassword(any(PasswordResetInternalDTO.class));
    }

    @Test
    @DisplayName("Deve rejeitar com 401 quando o cabeçalho X-Internal-Service-Key estiver ausente")
    void shouldReturnUnauthorizedWhenInternalKeyIsMissing() throws Exception {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("farm_owner", 10L, "NovaSenha@123");

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Acesso não autorizado. Chave interna ausente ou inválida."));

        verifyNoInteractions(internalAuthService);
    }

    @Test
    @DisplayName("Deve rejeitar com 401 quando o cabeçalho X-Internal-Service-Key for inválido")
    void shouldReturnUnauthorizedWhenInternalKeyIsInvalid() throws Exception {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("farm_owner", 10L, "NovaSenha@123");

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", "wrong-secret-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Acesso não autorizado. Chave interna ausente ou inválida."));

        verifyNoInteractions(internalAuthService);
    }

    @Test
    @DisplayName("Deve rejeitar com 400 quando a nova senha não atender aos requisitos de complexidade")
    void shouldReturnBadRequestWhenPasswordIsWeak() throws Exception {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("farm_owner", 10L, "fraca");

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.newPassword").exists());

        verifyNoInteractions(internalAuthService);
    }

    @Test
    @DisplayName("Deve rejeitar com 400 quando o accountType for inválido")
    void shouldReturnBadRequestWhenAccountTypeIsInvalid() throws Exception {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("invalid_type", 10L, "NovaSenha@123");

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.accountType").exists());

        verifyNoInteractions(internalAuthService);
    }

    @Test
    @DisplayName("Deve retornar 404 quando o usuário não for encontrado no banco de dados")
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("farm_owner", 999L, "NovaSenha@123");
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural não encontrado para o ID informado: 999"))
                .when(internalAuthService).resetPassword(any(PasswordResetInternalDTO.class));

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Service-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Produtor rural não encontrado para o ID informado: 999"));

        verify(internalAuthService).resetPassword(any(PasswordResetInternalDTO.class));
    }
}
