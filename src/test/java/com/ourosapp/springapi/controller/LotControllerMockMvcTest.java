package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.lot.LotRequestDTO;
import com.ourosapp.springapi.dto.lot.LotResponseDTO;
import com.ourosapp.springapi.dto.lot.LotUpdateDTO;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.LotService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
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
 * Testes de integração Web via MockMvc para o controlador {@link LotController}.
 */
@WebMvcTest(LotController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class LotControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LotService lotService;

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

    @Test
    @DisplayName("POST /lots - Deve cadastrar lote e retornar 201 Created com cabeçalho Location")
    void testCreateLotSuccess() throws Exception {
        LotRequestDTO request = new LotRequestDTO(
                50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
        );
        LotResponseDTO response = new LotResponseDTO(
                10L, 50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
        );

        when(lotService.createLot(any(LotRequestDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(post("/lots")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/lots/10")))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.received_chickens").value(50000))
                .andExpect(jsonPath("$.delivered_chickens").value(48500))
                .andExpect(jsonPath("$.date_birth").value("2026-09-01"))
                .andExpect(jsonPath("$.delivery_date").value("2026-10-15"))
                .andExpect(jsonPath("$.gain").value(2.8500))
                .andExpect(jsonPath("$.losts").value(1500))
                .andExpect(jsonPath("$.cost").value(12500.50))
                .andExpect(jsonPath("$.id_enterprise").value(1L))
                .andExpect(jsonPath("$.id_farm").value(2L));
    }

    @Test
    @DisplayName("POST /lots - Deve aceitar payload em formato camelCase (interoperabilidade)")
    void testCreateLotCamelCasePayloadSuccess() throws Exception {
        String camelCasePayload = """
                {
                    "receivedChickens": 50000,
                    "deliveredChickens": 48500,
                    "dateBirth": "2026-09-01",
                    "deliveryDate": "2026-10-15",
                    "gain": 2.8500,
                    "losts": 1500,
                    "cost": 12500.50,
                    "idEnterprise": 1,
                    "idFarm": 2
                }
                """;
        LotResponseDTO response = new LotResponseDTO(
                10L, 50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
        );

        when(lotService.createLot(any(LotRequestDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(post("/lots")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCasePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("POST /lots - Deve retornar 400 Bad Request quando campos obrigatórios estiverem ausentes ou inválidos")
    void testCreateLotInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "received_chickens": -50,
                    "date_birth": null,
                    "id_enterprise": null,
                    "id_farm": -1
                }
                """;

        mockMvc.perform(post("/lots")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /lots - Deve retornar 400 Bad Request quando delivered_chickens > received_chickens")
    void testCreateLotCrossValidationDeliveredExceedsReceived() throws Exception {
        String invalidPayload = """
                {
                    "received_chickens": 1000,
                    "delivered_chickens": 2000,
                    "date_birth": "2026-09-01",
                    "delivery_date": "2026-10-15",
                    "id_enterprise": 1,
                    "id_farm": 2
                }
                """;

        mockMvc.perform(post("/lots")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /lots - Deve retornar 400 Bad Request quando delivery_date for anterior a date_birth")
    void testCreateLotCrossValidationDeliveryDateBeforeDateBirth() throws Exception {
        String invalidPayload = """
                {
                    "received_chickens": 1000,
                    "date_birth": "2026-09-15",
                    "delivery_date": "2026-09-01",
                    "id_enterprise": 1,
                    "id_farm": 2
                }
                """;

        mockMvc.perform(post("/lots")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /lots - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateLotUnauthorized() throws Exception {
        LotRequestDTO request = new LotRequestDTO(
                50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
        );

        mockMvc.perform(post("/lots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /lots - Deve retornar 403 Forbidden quando usuário sem permissão")
    void testCreateLotForbidden() throws Exception {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 1L, 2L
        );
        when(lotService.createLot(any(LotRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/lots")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /lots - Deve retornar 409 Conflict quando ocorre violação de integridade")
    void testCreateLotConflict() throws Exception {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 1L, 2L
        );
        when(lotService.createLot(any(LotRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade"));

        mockMvc.perform(post("/lots")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /lots - Deve retornar lista de lotes com status 200 OK")
    void testGetLotsForUserSuccess() throws Exception {
        LotResponseDTO lot = new LotResponseDTO(
                1L, 50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
        );
        when(lotService.getLotsForUser(eq(2L), eq(1L), eq(mockPrincipal))).thenReturn(List.of(lot));

        mockMvc.perform(get("/lots")
                        .param("id_farm", "2")
                        .param("id_enterprise", "1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].received_chickens").value(50000));
    }

    @Test
    @DisplayName("GET /lots - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetLotsUnauthorized() throws Exception {
        mockMvc.perform(get("/lots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /lots/{id} - Deve retornar lote com status 200 OK")
    void testGetLotByIdSuccess() throws Exception {
        LotResponseDTO lot = new LotResponseDTO(
                1L, 50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
        );
        when(lotService.getLotById(eq(1L), eq(mockPrincipal))).thenReturn(lot);

        mockMvc.perform(get("/lots/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.received_chickens").value(50000));
    }

    @Test
    @DisplayName("GET /lots/{id} - Deve retornar 404 Not Found quando lote não existir")
    void testGetLotByIdNotFound() throws Exception {
        when(lotService.getLotById(eq(99L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote não encontrado"));

        mockMvc.perform(get("/lots/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /lots/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testGetLotByIdForbidden() throws Exception {
        when(lotService.getLotById(eq(1L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a este lote"));

        mockMvc.perform(get("/lots/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /lots/{id} - Deve atualizar parcialmente lote com status 200 OK")
    void testUpdateLotSuccess() throws Exception {
        LotUpdateDTO request = new LotUpdateDTO(
                null, 49000, null, LocalDate.of(2026, 10, 15),
                new BigDecimal("2.9500"), 1000, 13000.0
        );
        LotResponseDTO response = new LotResponseDTO(
                1L, 50000, 49000, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.9500"), 1000, 13000.0, 1L, 2L
        );

        when(lotService.updateLot(eq(1L), any(LotUpdateDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(patch("/lots/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered_chickens").value(49000))
                .andExpect(jsonPath("$.gain").value(2.9500));
    }

    @Test
    @DisplayName("PATCH /lots/{id} - Deve retornar 400 Bad Request quando payload for inválido")
    void testUpdateLotInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "received_chickens": -10,
                    "delivered_chickens": -5,
                    "gain": -1.0,
                    "cost": -100.0
                }
                """;

        mockMvc.perform(patch("/lots/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /lots/{id} - Deve retornar 404 Not Found quando lote não existir")
    void testUpdateLotNotFound() throws Exception {
        LotUpdateDTO request = new LotUpdateDTO(null, 49000, null, null, null, null, null);
        when(lotService.updateLot(eq(99L), any(LotUpdateDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote não encontrado"));

        mockMvc.perform(patch("/lots/99")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /lots/{id} - Deve remover lote e retornar 204 No Content")
    void testDeleteLotSuccess() throws Exception {
        doNothing().when(lotService).deleteLot(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/lots/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /lots/{id} - Deve retornar 404 Not Found quando lote não existir")
    void testDeleteLotNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote não encontrado"))
                .when(lotService).deleteLot(eq(99L), eq(mockPrincipal));

        mockMvc.perform(delete("/lots/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /lots/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testDeleteLotForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"))
                .when(lotService).deleteLot(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/lots/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /lots/{id} - Deve retornar 409 Conflict quando existirem registros vinculados")
    void testDeleteLotConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade ao remover lote"))
                .when(lotService).deleteLot(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/lots/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isConflict());
    }
}
