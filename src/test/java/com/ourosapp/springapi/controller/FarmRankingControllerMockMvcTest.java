package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.farm.FarmPodiumItemDTO;
import com.ourosapp.springapi.dto.farm.FarmPodiumRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmScoreUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmRankingService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link FarmRankingController}.
 */
@WebMvcTest(FarmRankingController.class)
@Import(SecurityConfig.class)
class FarmRankingControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FarmRankingService farmRankingService;

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
    @DisplayName("Deve retornar 200 OK com lista de granjas no ranking contendo apenas os dados essenciais")
    void deveRetornarTopRankingsComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("ADM");
        FarmRankingResponseDTO item = new FarmRankingResponseDTO(
                1L, "Granja Sol Poente", "Sudeste", 1.15, 1L
        );

        when(farmRankingService.getTopRankings(eq(1L), eq(10), any(UserPrincipal.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/farms/rankings")
                        .with(user(principal))
                        .param("id_enterprise", "1")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].farm_id").value(1))
                .andExpect(jsonPath("$[0].farm_name").value("Granja Sol Poente"))
                .andExpect(jsonPath("$[0].region").value("Sudeste"))
                .andExpect(jsonPath("$[0].score").value(1.15))
                .andExpect(jsonPath("$[0].rank_position").value(1))
                .andExpect(jsonPath("$[0].poultry_capacity").doesNotExist())
                .andExpect(jsonPath("$[0].chickens_now").doesNotExist())
                .andExpect(jsonPath("$[0].medal").doesNotExist());
    }

    @Test
    @DisplayName("Deve retornar 200 OK com posição individual da granja e dados essenciais")
    void deveRetornarPosicaoDaGranjaComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("COMPANY_EMPLOYEE");
        FarmRankingResponseDTO item = new FarmRankingResponseDTO(
                2L, "Granja Aurora", "Sul", 1.50, 3L
        );

        when(farmRankingService.getFarmPosition(eq(2L), any(UserPrincipal.class)))
                .thenReturn(item);

        mockMvc.perform(get("/farms/2/ranking")
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farm_id").value(2))
                .andExpect(jsonPath("$.farm_name").value("Granja Aurora"))
                .andExpect(jsonPath("$.region").value("Sul"))
                .andExpect(jsonPath("$.score").value(1.50))
                .andExpect(jsonPath("$.rank_position").value(3));
    }

    @Test
    @DisplayName("Deve retornar 200 OK com pódio relativo da fazenda")
    void deveRetornarPodiumDaFazendaComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("FARM_OWNER");
        FarmPodiumItemDTO above = new FarmPodiumItemDTO(1L, "Granja 1", "Sudeste", 1.0, 1L, "ABOVE");
        FarmPodiumItemDTO current = new FarmPodiumItemDTO(2L, "Granja 2", "Sudeste", 1.2, 2L, "CURRENT");
        FarmPodiumItemDTO below = new FarmPodiumItemDTO(3L, "Granja 3", "Sudeste", 1.4, 3L, "BELOW");

        FarmPodiumRankingResponseDTO podiumResponse = new FarmPodiumRankingResponseDTO(
                2L, List.of(above, current, below)
        );

        when(farmRankingService.getFarmPodiumRanking(eq(2L), any(UserPrincipal.class)))
                .thenReturn(podiumResponse);

        mockMvc.perform(get("/farms/2/ranking/podium")
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_farm_id").value(2))
                .andExpect(jsonPath("$.ranking_podium[0].relation").value("ABOVE"))
                .andExpect(jsonPath("$.ranking_podium[0].farm_name").value("Granja 1"))
                .andExpect(jsonPath("$.ranking_podium[0].region").value("Sudeste"))
                .andExpect(jsonPath("$.ranking_podium[0].score").value(1.0))
                .andExpect(jsonPath("$.ranking_podium[1].relation").value("CURRENT"))
                .andExpect(jsonPath("$.ranking_podium[1].farm_id").value(2))
                .andExpect(jsonPath("$.ranking_podium[2].relation").value("BELOW"));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando produtor consultar pódio de outra fazenda")
    void deveRetornar403QuandoProdutorConsultarPodiumAlheio() throws Exception {
        UserPrincipal principal = createMockUser("FARM_OWNER");

        when(farmRankingService.getFarmPodiumRanking(eq(99L), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a esta fazenda"));

        mockMvc.perform(get("/farms/99/ranking/podium")
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a granja não existir")
    void deveRetornar404QuandoGranjaNaoExistir() throws Exception {
        UserPrincipal principal = createMockUser("COMPANY_EMPLOYEE");

        when(farmRankingService.getFarmPosition(eq(999L), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada"));

        mockMvc.perform(get("/farms/999/ranking")
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao atualizar pontuação de ranking de uma fazenda")
    void deveAtualizarScoreComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("COMPANY_EMPLOYEE");
        FarmScoreUpdateDTO dto = new FarmScoreUpdateDTO(1.23);
        FarmRankingResponseDTO responseDTO = new FarmRankingResponseDTO(
                1L, "Granja Boa Vista", "Sudeste", 1.23, 1L
        );

        when(farmRankingService.updateFarmScore(eq(1L), any(FarmScoreUpdateDTO.class), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(put("/farms/1/ranking/score")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1.23))
                .andExpect(jsonPath("$.farm_name").value("Granja Boa Vista"))
                .andExpect(jsonPath("$.region").value("Sudeste"))
                .andExpect(jsonPath("$.rank_position").value(1));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request ao informar score negativo")
    void deveRetornar400QuandoScoreInvalido() throws Exception {
        UserPrincipal principal = createMockUser("ADM");
        FarmScoreUpdateDTO dto = new FarmScoreUpdateDTO(-10.0);

        mockMvc.perform(put("/farms/1/ranking/score")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 200 OK ao disparar sincronização de ranking")
    void deveSincronizarRankingComSucesso() throws Exception {
        UserPrincipal principal = createMockUser("ADM");

        when(farmRankingService.syncAllFarmsToRanking(eq(1L), any(UserPrincipal.class)))
                .thenReturn(5L);

        mockMvc.perform(post("/farms/rankings/sync")
                        .with(user(principal))
                        .param("id_enterprise", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced_farms").value(5))
                .andExpect(jsonPath("$.message").value("Sincronização de ranking concluída com sucesso"));
    }
}
