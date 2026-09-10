package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryRequestDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryResponseDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryUpdateDTO;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import com.ourosapp.springapi.service.WaterRegistryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link WaterRegistryController}.
 */
@WebMvcTest(WaterRegistryController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class WaterRegistryControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WaterRegistryService waterRegistryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UserPrincipal mockPrincipal = new UserPrincipal(
            1L,
            "admin@ouros.com",
            "password123",
            "ADM",
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    /**
     * Testa POST /water-registries com dados válidos esperando 201 Created e Location.
     */
    @Test
    @DisplayName("POST /water-registries - Deve cadastrar medição de água e retornar 201 Created com cabeçalho Location")
    void testCreateWaterRegistrySuccess() throws Exception {
        WaterRegistryRequestDTO request = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.5000"),
                new BigDecimal("120.8000"),
                1L
        );
        WaterRegistryResponseDTO response = new WaterRegistryResponseDTO(
                10L,
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.5000"),
                new BigDecimal("120.8000"),
                1L
        );

        when(waterRegistryService.createWaterRegistry(any(WaterRegistryRequestDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(post("/water-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/water-registries/10")))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.registration_date").value("2026-09-10"))
                .andExpect(jsonPath("$.start_hydrometer").value(100.5000))
                .andExpect(jsonPath("$.end_hydrometer").value(120.8000))
                .andExpect(jsonPath("$.id_farm").value(1L));
    }

    /**
     * Testa POST /water-registries com payload em formato camelCase esperando 201 Created.
     */
    @Test
    @DisplayName("POST /water-registries - Deve aceitar payload em camelCase (interoperabilidade)")
    void testCreateWaterRegistryCamelCaseSuccess() throws Exception {
        String camelCasePayload = """
                {
                    "registrationDate": "2026-09-10",
                    "startHydrometer": 100.50,
                    "endHydrometer": 120.80,
                    "idFarm": 1
                }
                """;
        WaterRegistryResponseDTO response = new WaterRegistryResponseDTO(
                1L,
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.50"),
                new BigDecimal("120.80"),
                1L
        );

        when(waterRegistryService.createWaterRegistry(any(WaterRegistryRequestDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(post("/water-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCasePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(waterRegistryService).createWaterRegistry(argThat(dto ->
                LocalDate.of(2026, 9, 10).equals(dto.registrationDate())
                        && new BigDecimal("100.50").equals(dto.startHydrometer())
                        && new BigDecimal("120.80").equals(dto.endHydrometer())
                        && Long.valueOf(1L).equals(dto.idFarm())
        ), eq(mockPrincipal));
    }

    /**
     * Testa POST /water-registries com payload inválido esperando 400 Bad Request.
     */
    @Test
    @DisplayName("POST /water-registries - Deve retornar 400 Bad Request quando payload for inválido")
    void testCreateWaterRegistryInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "registration_date": null,
                    "start_hydrometer": -10.00
                }
                """;

        mockMvc.perform(post("/water-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    /**
     * Testa POST /water-registries sem autenticação esperando 401 Unauthorized.
     */
    @Test
    @DisplayName("POST /water-registries - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateWaterRegistryUnauthorized() throws Exception {
        WaterRegistryRequestDTO request = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                1L
        );

        mockMvc.perform(post("/water-registries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Testa POST /water-registries sem permissão esperando 403 Forbidden.
     */
    @Test
    @DisplayName("POST /water-registries - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testCreateWaterRegistryForbidden() throws Exception {
        WaterRegistryRequestDTO request = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                1L
        );
        when(waterRegistryService.createWaterRegistry(any(WaterRegistryRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/water-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * Testa POST /water-registries quando ocorre conflito esperando 409 Conflict.
     */
    @Test
    @DisplayName("POST /water-registries - Deve retornar 409 Conflict quando ocorre conflito de integridade")
    void testCreateWaterRegistryConflict() throws Exception {
        WaterRegistryRequestDTO request = new WaterRegistryRequestDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                1L
        );
        when(waterRegistryService.createWaterRegistry(any(WaterRegistryRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade"));

        mockMvc.perform(post("/water-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    /**
     * Testa GET /water-registries sem parâmetros esperando 200 OK.
     */
    @Test
    @DisplayName("GET /water-registries - Deve retornar lista de registros com status 200 OK")
    void testGetWaterRegistriesSuccess() throws Exception {
        WaterRegistryResponseDTO item = new WaterRegistryResponseDTO(
                1L,
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                1L
        );
        when(waterRegistryService.getWaterRegistriesForUser(isNull(), eq(mockPrincipal)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/water-registries")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].start_hydrometer").value(100.00));
    }

    /**
     * Testa GET /water-registries com filtro por farm_id esperando 200 OK.
     */
    @Test
    @DisplayName("GET /water-registries?farm_id=1 - Deve filtrar por fazenda e retornar 200 OK")
    void testGetWaterRegistriesWithFarmIdSuccess() throws Exception {
        WaterRegistryResponseDTO item = new WaterRegistryResponseDTO(
                1L,
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                1L
        );
        when(waterRegistryService.getWaterRegistriesForUser(eq(1L), eq(mockPrincipal)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/water-registries")
                        .param("farm_id", "1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].id_farm").value(1L));
    }

    /**
     * Testa GET /water-registries sem autenticação esperando 401 Unauthorized.
     */
    @Test
    @DisplayName("GET /water-registries - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetWaterRegistriesUnauthorized() throws Exception {
        mockMvc.perform(get("/water-registries"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Testa GET /water-registries/{id} com registro existente esperando 200 OK.
     */
    @Test
    @DisplayName("GET /water-registries/{id} - Deve retornar registro de água com status 200 OK")
    void testGetWaterRegistryByIdSuccess() throws Exception {
        WaterRegistryResponseDTO response = new WaterRegistryResponseDTO(
                1L,
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.00"),
                new BigDecimal("120.00"),
                1L
        );
        when(waterRegistryService.getWaterRegistryById(eq(1L), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(get("/water-registries/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.start_hydrometer").value(100.00));
    }

    /**
     * Testa GET /water-registries/{id} inexistente esperando 404 Not Found.
     */
    @Test
    @DisplayName("GET /water-registries/{id} - Deve retornar 404 Not Found quando registro não existir")
    void testGetWaterRegistryByIdNotFound() throws Exception {
        when(waterRegistryService.getWaterRegistryById(eq(99L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de água não encontrado"));

        mockMvc.perform(get("/water-registries/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa GET /water-registries/{id} sem permissão esperando 403 Forbidden.
     */
    @Test
    @DisplayName("GET /water-registries/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testGetWaterRegistryByIdForbidden() throws Exception {
        when(waterRegistryService.getWaterRegistryById(eq(1L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(get("/water-registries/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }

    /**
     * Testa PATCH /water-registries/{id} com sucesso esperando 200 OK.
     */
    @Test
    @DisplayName("PATCH /water-registries/{id} - Deve atualizar parcialmente o registro e retornar 200 OK")
    void testUpdateWaterRegistrySuccess() throws Exception {
        WaterRegistryUpdateDTO request = new WaterRegistryUpdateDTO(
                new BigDecimal("150.00"),
                null,
                null
        );
        WaterRegistryResponseDTO response = new WaterRegistryResponseDTO(
                1L,
                LocalDate.of(2026, 9, 10),
                new BigDecimal("100.00"),
                new BigDecimal("150.00"),
                1L
        );

        when(waterRegistryService.updateWaterRegistry(eq(1L), any(WaterRegistryUpdateDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(patch("/water-registries/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.end_hydrometer").value(150.00));
    }

    /**
     * Testa PATCH /water-registries/{id} com payload inválido esperando 400 Bad Request.
     */
    @Test
    @DisplayName("PATCH /water-registries/{id} - Deve retornar 400 Bad Request quando payload for inválido")
    void testUpdateWaterRegistryInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "end_hydrometer": -5.00
                }
                """;

        mockMvc.perform(patch("/water-registries/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    /**
     * Testa PATCH /water-registries/{id} quando registro não for encontrado esperando 404 Not Found.
     */
    @Test
    @DisplayName("PATCH /water-registries/{id} - Deve retornar 404 Not Found quando registro não existir")
    void testUpdateWaterRegistryNotFound() throws Exception {
        WaterRegistryUpdateDTO request = new WaterRegistryUpdateDTO(new BigDecimal("150.00"), null, null);
        when(waterRegistryService.updateWaterRegistry(eq(99L), any(WaterRegistryUpdateDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de água não encontrado"));

        mockMvc.perform(patch("/water-registries/99")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa DELETE /water-registries/{id} com sucesso esperando 204 No Content.
     */
    @Test
    @DisplayName("DELETE /water-registries/{id} - Deve remover registro e retornar status 204 No Content")
    void testDeleteWaterRegistrySuccess() throws Exception {
        doNothing().when(waterRegistryService).deleteWaterRegistry(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/water-registries/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    /**
     * Testa DELETE /water-registries/{id} quando registro não existe esperando 404 Not Found.
     */
    @Test
    @DisplayName("DELETE /water-registries/{id} - Deve retornar 404 Not Found quando registro não existir")
    void testDeleteWaterRegistryNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de água não encontrado"))
                .when(waterRegistryService).deleteWaterRegistry(eq(99L), eq(mockPrincipal));

        mockMvc.perform(delete("/water-registries/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa DELETE /water-registries/{id} sem permissão esperando 403 Forbidden.
     */
    @Test
    @DisplayName("DELETE /water-registries/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testDeleteWaterRegistryForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"))
                .when(waterRegistryService).deleteWaterRegistry(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/water-registries/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }

    /**
     * Testa DELETE /water-registries/{id} quando há conflito de integridade referencial esperando 409 Conflict.
     */
    @Test
    @DisplayName("DELETE /water-registries/{id} - Deve retornar 409 Conflict quando existirem dados vinculados")
    void testDeleteWaterRegistryConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade"))
                .when(waterRegistryService).deleteWaterRegistry(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/water-registries/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isConflict());
    }
}
