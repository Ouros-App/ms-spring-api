package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftRequestDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftResponseDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftUpdateDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.ChickenLeftService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link ChickenLeftController}.
 */
@WebMvcTest(ChickenLeftController.class)
@Import(SecurityConfig.class)
class ChickenLeftControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChickenLeftService chickenLeftService;

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

    /**
     * Testa POST /chicken-left com dados válidos esperando 201 Created e Location.
     */
    @Test
    @DisplayName("POST /chicken-left - Deve cadastrar saída de aves e retornar 201 Created com cabeçalho Location")
    void testCreateChickenLeftSuccess() throws Exception {
        ChickenLeftRequestDTO request = new ChickenLeftRequestDTO(
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );
        ChickenLeftResponseDTO response = new ChickenLeftResponseDTO(
                10L,
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );

        when(chickenLeftService.createChickenLeft(any(ChickenLeftRequestDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(post("/chicken-left")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/chicken-left/10")))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.chickens_count").value(500))
                .andExpect(jsonPath("$.exit_date").value("2026-09-20"))
                .andExpect(jsonPath("$.id_farm").value(1L));
    }

    /**
     * Testa POST /chicken-left com payload em formato camelCase esperando 201 Created.
     */
    @Test
    @DisplayName("POST /chicken-left - Deve aceitar payload em camelCase (interoperabilidade)")
    void testCreateChickenLeftCamelCaseSuccess() throws Exception {
        String camelCasePayload = """
                {
                    "chickensCount": 500,
                    "exitDate": "2026-09-20",
                    "idFarm": 1
                }
                """;
        ChickenLeftResponseDTO response = new ChickenLeftResponseDTO(
                1L,
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );

        when(chickenLeftService.createChickenLeft(any(ChickenLeftRequestDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(post("/chicken-left")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCasePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(chickenLeftService).createChickenLeft(argThat(dto ->
                Integer.valueOf(500).equals(dto.chickensCount())
                        && LocalDate.of(2026, 9, 20).equals(dto.exitDate())
                        && Long.valueOf(1L).equals(dto.idFarm())
        ), eq(mockPrincipal));
    }

    /**
     * Testa POST /chicken-left com payload inválido esperando 400 Bad Request.
     */
    @Test
    @DisplayName("POST /chicken-left - Deve retornar 400 Bad Request quando payload for inválido")
    void testCreateChickenLeftInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "chickens_count": -5,
                    "exit_date": null
                }
                """;

        mockMvc.perform(post("/chicken-left")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    /**
     * Testa POST /chicken-left sem autenticação esperando 401 Unauthorized.
     */
    @Test
    @DisplayName("POST /chicken-left - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateChickenLeftUnauthorized() throws Exception {
        ChickenLeftRequestDTO request = new ChickenLeftRequestDTO(
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );

        mockMvc.perform(post("/chicken-left")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Testa POST /chicken-left sem permissão esperando 403 Forbidden.
     */
    @Test
    @DisplayName("POST /chicken-left - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testCreateChickenLeftForbidden() throws Exception {
        ChickenLeftRequestDTO request = new ChickenLeftRequestDTO(
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );
        when(chickenLeftService.createChickenLeft(any(ChickenLeftRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/chicken-left")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /**
     * Testa POST /chicken-left quando ocorre conflito esperando 409 Conflict.
     */
    @Test
    @DisplayName("POST /chicken-left - Deve retornar 409 Conflict quando ocorre conflito de integridade")
    void testCreateChickenLeftConflict() throws Exception {
        ChickenLeftRequestDTO request = new ChickenLeftRequestDTO(
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );
        when(chickenLeftService.createChickenLeft(any(ChickenLeftRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade"));

        mockMvc.perform(post("/chicken-left")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    /**
     * Testa GET /chicken-left sem parâmetros esperando 200 OK.
     */
    @Test
    @DisplayName("GET /chicken-left - Deve retornar lista de registros com status 200 OK")
    void testGetChickenLeftsSuccess() throws Exception {
        ChickenLeftResponseDTO item = new ChickenLeftResponseDTO(
                1L,
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );
        when(chickenLeftService.getChickenLeftsForUser(isNull(), eq(mockPrincipal)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/chicken-left")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].chickens_count").value(500));
    }

    /**
     * Testa GET /chicken-left com filtro por farm_id esperando 200 OK.
     */
    @Test
    @DisplayName("GET /chicken-left?farm_id=1 - Deve filtrar por fazenda e retornar 200 OK")
    void testGetChickenLeftsWithFarmIdSuccess() throws Exception {
        ChickenLeftResponseDTO item = new ChickenLeftResponseDTO(
                1L,
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );
        when(chickenLeftService.getChickenLeftsForUser(eq(1L), eq(mockPrincipal)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/chicken-left")
                        .param("farm_id", "1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].id_farm").value(1L));
    }

    /**
     * Testa GET /chicken-left sem autenticação esperando 401 Unauthorized.
     */
    @Test
    @DisplayName("GET /chicken-left - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetChickenLeftsUnauthorized() throws Exception {
        mockMvc.perform(get("/chicken-left"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Testa GET /chicken-left/{id} com registro existente esperando 200 OK.
     */
    @Test
    @DisplayName("GET /chicken-left/{id} - Deve retornar registro de saída com status 200 OK")
    void testGetChickenLeftByIdSuccess() throws Exception {
        ChickenLeftResponseDTO response = new ChickenLeftResponseDTO(
                1L,
                500,
                LocalDate.of(2026, 9, 20),
                1L
        );
        when(chickenLeftService.getChickenLeftById(eq(1L), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(get("/chicken-left/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.chickens_count").value(500));
    }

    /**
     * Testa GET /chicken-left/{id} inexistente esperando 404 Not Found.
     */
    @Test
    @DisplayName("GET /chicken-left/{id} - Deve retornar 404 Not Found quando registro não existir")
    void testGetChickenLeftByIdNotFound() throws Exception {
        when(chickenLeftService.getChickenLeftById(eq(99L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de saída de aves não encontrado"));

        mockMvc.perform(get("/chicken-left/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa GET /chicken-left/{id} sem permissão esperando 403 Forbidden.
     */
    @Test
    @DisplayName("GET /chicken-left/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testGetChickenLeftByIdForbidden() throws Exception {
        when(chickenLeftService.getChickenLeftById(eq(1L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(get("/chicken-left/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }

    /**
     * Testa PATCH /chicken-left/{id} com sucesso esperando 200 OK.
     */
    @Test
    @DisplayName("PATCH /chicken-left/{id} - Deve atualizar parcialmente o registro e retornar 200 OK")
    void testUpdateChickenLeftSuccess() throws Exception {
        ChickenLeftUpdateDTO request = new ChickenLeftUpdateDTO(
                600,
                LocalDate.of(2026, 9, 21)
        );
        ChickenLeftResponseDTO response = new ChickenLeftResponseDTO(
                1L,
                600,
                LocalDate.of(2026, 9, 21),
                1L
        );

        when(chickenLeftService.updateChickenLeft(eq(1L), any(ChickenLeftUpdateDTO.class), eq(mockPrincipal)))
                .thenReturn(response);

        mockMvc.perform(patch("/chicken-left/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.chickens_count").value(600))
                .andExpect(jsonPath("$.exit_date").value("2026-09-21"));
    }

    /**
     * Testa PATCH /chicken-left/{id} com payload inválido esperando 400 Bad Request.
     */
    @Test
    @DisplayName("PATCH /chicken-left/{id} - Deve retornar 400 Bad Request quando payload for inválido")
    void testUpdateChickenLeftInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "chickens_count": -5
                }
                """;

        mockMvc.perform(patch("/chicken-left/1")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    /**
     * Testa PATCH /chicken-left/{id} quando registro não for encontrado esperando 404 Not Found.
     */
    @Test
    @DisplayName("PATCH /chicken-left/{id} - Deve retornar 404 Not Found quando registro não existir")
    void testUpdateChickenLeftNotFound() throws Exception {
        ChickenLeftUpdateDTO request = new ChickenLeftUpdateDTO(600, null);
        when(chickenLeftService.updateChickenLeft(eq(99L), any(ChickenLeftUpdateDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de saída de aves não encontrado"));

        mockMvc.perform(patch("/chicken-left/99")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa DELETE /chicken-left/{id} com sucesso esperando 204 No Content.
     */
    @Test
    @DisplayName("DELETE /chicken-left/{id} - Deve remover registro e retornar status 204 No Content")
    void testDeleteChickenLeftSuccess() throws Exception {
        doNothing().when(chickenLeftService).deleteChickenLeft(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/chicken-left/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    /**
     * Testa DELETE /chicken-left/{id} quando registro não existe esperando 404 Not Found.
     */
    @Test
    @DisplayName("DELETE /chicken-left/{id} - Deve retornar 404 Not Found quando registro não existir")
    void testDeleteChickenLeftNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de saída de aves não encontrado"))
                .when(chickenLeftService).deleteChickenLeft(eq(99L), eq(mockPrincipal));

        mockMvc.perform(delete("/chicken-left/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa DELETE /chicken-left/{id} sem permissão esperando 403 Forbidden.
     */
    @Test
    @DisplayName("DELETE /chicken-left/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void testDeleteChickenLeftForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"))
                .when(chickenLeftService).deleteChickenLeft(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/chicken-left/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }

    /**
     * Testa DELETE /chicken-left/{id} quando há conflito de integridade referencial esperando 409 Conflict.
     */
    @Test
    @DisplayName("DELETE /chicken-left/{id} - Deve retornar 409 Conflict quando existirem dados vinculados")
    void testDeleteChickenLeftConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade"))
                .when(chickenLeftService).deleteChickenLeft(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/chicken-left/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isConflict());
    }
}
