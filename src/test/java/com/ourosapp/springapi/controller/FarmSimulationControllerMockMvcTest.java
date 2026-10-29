package com.ourosapp.springapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.farm.FarmRankingSimulationResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmSimulationService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.List;

@WebMvcTest(FarmSimulationController.class)
@Import(SecurityConfig.class)
@DisplayName("Testes MockMvc do FarmSimulationController")
class FarmSimulationControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FarmSimulationService farmSimulationService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private UserPrincipal employeePrincipal;
    private FarmSimulationResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        employeePrincipal = new UserPrincipal(
                10L,
                "func@integradora.com",
                "password123",
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        FarmRankingSimulationResponseDTO rankingSim = new FarmRankingSimulationResponseDTO(
                1L, "Granja Sol Nascente", 12000.0, 5L, "BRONZE", 9500.0, 2L, "OURO", 3L, 10L, 20.0
        );

        sampleResponse = new FarmSimulationResponseDTO(
                100L, 1L, 5L, "LT-2026-042", 12000.0, 9500.0, 0.015, 0.85, 25000, 3200.0,
                LocalDateTime.now(), LocalDateTime.now(), rankingSim
        );
    }

    @Test
    @DisplayName("GET /farms/simulations deve retornar 200 OK com lista de simulações")
    void getSimulations_returns200() throws Exception {
        when(farmSimulationService.getSimulations(any(), any(), any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/farms/simulations").with(user(employeePrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].id_farm").value(1))
                .andExpect(jsonPath("$[0].id_lot").value("LT-2026-042"))
                .andExpect(jsonPath("$[0].simulated_ranking.simulated_league").value("OURO"));
    }

    @Test
    @DisplayName("POST /farms/simulations deve criar simulação e retornar 201 Created")
    void createSimulation_returns201() throws Exception {
        FarmSimulationRequestDTO request = new FarmSimulationRequestDTO(
                1L, "LT-2026-042", 12000.0, 9500.0, 0.015, 0.85, 25000, 3200.0
        );
        when(farmSimulationService.createSimulation(any(FarmSimulationRequestDTO.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/farms/simulations")
                        .with(user(employeePrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.target_cgi").value(9500.0));
    }

    @Test
    @DisplayName("PATCH /farms/simulations/{id} deve atualizar simulação e retornar 200 OK")
    void updateSimulation_returns200() throws Exception {
        FarmSimulationUpdateDTO update = new FarmSimulationUpdateDTO(
                null, "LT-2026-099", null, 8800.0, null, null, null, null
        );
        when(farmSimulationService.updateSimulation(eq(100L), any(FarmSimulationUpdateDTO.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/farms/simulations/100")
                        .with(user(employeePrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    @DisplayName("DELETE /farms/simulations/{id} deve remover simulação e retornar 204 No Content")
    void deleteSimulation_returns204() throws Exception {
        doNothing().when(farmSimulationService).deleteSimulation(eq(100L), any());

        mockMvc.perform(delete("/farms/simulations/100").with(user(employeePrincipal)))
                .andExpect(status().isNoContent());
    }
}
