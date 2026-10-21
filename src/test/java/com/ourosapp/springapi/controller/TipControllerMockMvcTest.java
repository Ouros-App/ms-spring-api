package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.tip.TipRequestDTO;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.dto.tip.TipUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.TipService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link TipController}.
 */
@WebMvcTest(TipController.class)
@Import(SecurityConfig.class)
class TipControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TipService tipService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UserPrincipal mockPrincipal = new UserPrincipal(
            1L,
            "admin@ouros.com",
            "password123",
            "ADM",
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    @Test
    @DisplayName("POST /tips - Deve cadastrar dica técnica e retornar 201 Created com Location")
    void deveCadastrarDicaComSucesso() throws Exception {
        TipRequestDTO request = new TipRequestDTO("Manter ventilação mínima no galpão.", 1L, List.of(1L, 2L));
        TipResponseDTO response = new TipResponseDTO(
                10L,
                "Manter ventilação mínima no galpão.",
                1L,
                List.of("Manejo de Ambiência", "Sanitização"),
                0,
                0.0
        );

        when(tipService.createTip(any(TipRequestDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(post("/tips")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/tips/10")))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.tip").value("Manter ventilação mínima no galpão."))
                .andExpect(jsonPath("$.id_farm").value(1L))
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.total_reviews").value(0))
                .andExpect(jsonPath("$.average_rating").value(0.0));
    }

    @Test
    @DisplayName("POST /tips - Deve retornar 400 Bad Request se payload for inválido")
    void deveRetornarBadRequestQuandoPayloadInvalido() throws Exception {
        TipRequestDTO invalidRequest = new TipRequestDTO("", null, null);

        mockMvc.perform(post("/tips")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /tips - Deve listar dicas e retornar 200 OK")
    void deveListarDicasComSucesso() throws Exception {
        List<TipResponseDTO> responses = List.of(
                new TipResponseDTO(1L, "Dica 1", 1L, List.of("Ambiência"), 2, 4.5),
                new TipResponseDTO(2L, "Dica 2", 1L, List.of("Nutrição"), 0, 0.0)
        );

        when(tipService.getTipsForUser(eq(null), eq(mockPrincipal))).thenReturn(responses);

        mockMvc.perform(get("/tips")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tip").value("Dica 1"))
                .andExpect(jsonPath("$[0].total_reviews").value(2))
                .andExpect(jsonPath("$[0].average_rating").value(4.5));
    }

    @Test
    @DisplayName("GET /tips?farm_id=1 - Deve listar dicas filtrando por fazenda")
    void deveListarDicasComFiltroDeFazenda() throws Exception {
        List<TipResponseDTO> responses = List.of(
                new TipResponseDTO(1L, "Dica 1", 1L, List.of("Ambiência"), 1, 5.0)
        );

        when(tipService.getTipsForUser(eq(1L), eq(mockPrincipal))).thenReturn(responses);

        mockMvc.perform(get("/tips")
                        .param("farm_id", "1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /tips/{id} - Deve retornar detalhes de uma dica específica")
    void deveBuscarDicaPorIdComSucesso() throws Exception {
        TipResponseDTO response = new TipResponseDTO(1L, "Dica detalhada", 1L, List.of("Biosseguridade"), 1, 4.0);

        when(tipService.getTipById(eq(1L), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(get("/tips/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.tip").value("Dica detalhada"));
    }

    @Test
    @DisplayName("PATCH /tips/{id} - Deve atualizar parcialmente uma dica técnica")
    void deveAtualizarDicaComSucesso() throws Exception {
        TipUpdateDTO updateDTO = new TipUpdateDTO("Dica atualizada", List.of(2L));
        TipResponseDTO response = new TipResponseDTO(1L, "Dica atualizada", 1L, List.of("Nutrição"), 0, 0.0);

        when(tipService.updateTip(eq(1L), any(TipUpdateDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(patch("/tips/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.tip").value("Dica atualizada"));
    }

    @Test
    @DisplayName("DELETE /tips/{id} - Deve remover dica técnica e retornar 204 No Content")
    void deveRemoverDicaComSucesso() throws Exception {
        doNothing().when(tipService).deleteTip(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/tips/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }
}
