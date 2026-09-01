package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.AddressRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmUpdateDTO;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link FarmController}.
 */
@WebMvcTest(FarmController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class FarmControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FarmService farmService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("POST /farms - Deve cadastrar fazenda com id_address e retornar 201 Created com cabeçalho Location")
    void testCreateFarmWithIdAddressSuccess() throws Exception {
        FarmRequestDTO request = new FarmRequestDTO(
                "Fazenda Santa Maria",
                new BigDecimal("150.50"),
                "Sudeste",
                50000,
                "Gleba 4",
                1L,
                null,
                2L
        );
        FarmResponseDTO response = new FarmResponseDTO(
                10L,
                "Fazenda Santa Maria",
                new BigDecimal("150.50"),
                "Sudeste",
                50000,
                "Gleba 4",
                1L,
                2L
        );

        when(farmService.createFarm(any(FarmRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/farms/10")))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Fazenda Santa Maria"))
                .andExpect(jsonPath("$.area_property").value(150.50))
                .andExpect(jsonPath("$.region").value("Sudeste"))
                .andExpect(jsonPath("$.poultry_capacity").value(50000))
                .andExpect(jsonPath("$.place").value("Gleba 4"))
                .andExpect(jsonPath("$.id_address").value(1L))
                .andExpect(jsonPath("$.id_enterprise").value(2L));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /farms - Deve cadastrar fazenda com objeto address embutido (nested) e retornar 201 Created")
    void testCreateFarmWithNestedAddressSuccess() throws Exception {
        AddressRequestDTO address = new AddressRequestDTO("12345678", "SP", "Campinas", "100", "BR");
        FarmRequestDTO request = new FarmRequestDTO(
                "Fazenda Santa Maria",
                new BigDecimal("150.50"),
                "Sudeste",
                50000,
                "Gleba 4",
                null,
                address,
                2L
        );
        FarmResponseDTO response = new FarmResponseDTO(
                10L,
                "Fazenda Santa Maria",
                new BigDecimal("150.50"),
                "Sudeste",
                50000,
                "Gleba 4",
                5L,
                2L
        );

        when(farmService.createFarm(any(FarmRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.id_address").value(5L));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /farms - Deve aceitar payload em camelCase (interoperabilidade)")
    void testCreateFarmCamelCasePayloadSuccess() throws Exception {
        String camelCasePayload = """
                {
                    "name": "Fazenda Camel",
                    "areaProperty": 120.00,
                    "region": "Sul",
                    "poultryCapacity": 30000,
                    "place": "Setor A",
                    "idAddress": 1,
                    "idEnterprise": 2
                }
                """;
        FarmResponseDTO response = new FarmResponseDTO(
                1L, "Fazenda Camel", new BigDecimal("120.00"), "Sul", 30000, "Setor A", 1L, 2L
        );

        when(farmService.createFarm(any(FarmRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCasePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fazenda Camel"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /farms - Deve retornar 400 Bad Request quando payload for inválido")
    void testCreateFarmInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "name": "",
                    "area_property": -10,
                    "region": "",
                    "poultry_capacity": -5,
                    "place": "",
                    "id_enterprise": null
                }
                """;

        mockMvc.perform(post("/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /farms - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateFarmUnauthorized() throws Exception {
        FarmRequestDTO request = new FarmRequestDTO(
                "Fazenda", new BigDecimal("100"), "Sul", 1000, "Local", 1L, null, 2L
        );

        mockMvc.perform(post("/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /farms - Deve retornar lista de fazendas com status 200 OK")
    void testGetFarmsForUserSuccess() throws Exception {
        FarmResponseDTO farm1 = new FarmResponseDTO(
                1L, "Fazenda 1", new BigDecimal("100.00"), "Sul", 10000, "Local 1", 1L, 2L
        );
        when(farmService.getFarmsForUser(any())).thenReturn(List.of(farm1));

        mockMvc.perform(get("/farms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Fazenda 1"));
    }

    @Test
    @DisplayName("GET /farms - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetFarmsUnauthorized() throws Exception {
        mockMvc.perform(get("/farms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /farms/{id} - Deve retornar fazenda com status 200 OK")
    void testGetFarmByIdSuccess() throws Exception {
        FarmResponseDTO farm = new FarmResponseDTO(
                1L, "Fazenda 1", new BigDecimal("100.00"), "Sul", 10000, "Local 1", 1L, 2L
        );
        when(farmService.getFarmById(1L)).thenReturn(farm);

        mockMvc.perform(get("/farms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Fazenda 1"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /farms/{id} - Deve retornar 404 Not Found quando fazenda não existir")
    void testGetFarmByIdNotFound() throws Exception {
        when(farmService.getFarmById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada"));

        mockMvc.perform(get("/farms/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /farms/{id} - Deve atualizar parcialmente a fazenda com status 200 OK")
    void testUpdateFarmSuccess() throws Exception {
        FarmUpdateDTO request = new FarmUpdateDTO("Novo Nome", new BigDecimal("200.00"), null, null, null);
        FarmResponseDTO response = new FarmResponseDTO(
                1L, "Novo Nome", new BigDecimal("200.00"), "Sul", 10000, "Local 1", 1L, 2L
        );

        when(farmService.updateFarm(eq(1L), any(FarmUpdateDTO.class))).thenReturn(response);

        mockMvc.perform(patch("/farms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Nome"))
                .andExpect(jsonPath("$.area_property").value(200.00));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /farms/{id} - Deve retornar 400 Bad Request quando campos do PATCH forem inválidos")
    void testUpdateFarmInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "area_property": -50.00,
                    "poultry_capacity": -10
                }
                """;

        mockMvc.perform(patch("/farms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /farms/{id} - Deve remover fazenda e retornar status 204 No Content")
    void testDeleteFarmSuccess() throws Exception {
        doNothing().when(farmService).deleteFarm(1L);

        mockMvc.perform(delete("/farms/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /farms/{id} - Deve retornar 404 Not Found quando fazenda a ser removida não existir")
    void testDeleteFarmNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada"))
                .when(farmService).deleteFarm(99L);

        mockMvc.perform(delete("/farms/99"))
                .andExpect(status().isNotFound());
    }
}
