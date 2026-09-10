package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryRequestDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryResponseDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryUpdateDTO;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.EnergyRegistryService;
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
 * Testes de integração Web via MockMvc para o controlador {@link EnergyRegistryController}.
 */
@WebMvcTest(EnergyRegistryController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class EnergyRegistryControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnergyRegistryService energyRegistryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UserPrincipal mockPrincipal = new UserPrincipal(
            1L,
            "admin@ouros.com",
            "senha123",
            "ADM",
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    /** Verifica a criação de um registro de energia com resposta HTTP 201. */
    @Test
    @DisplayName("POST /energy-registries - Deve retornar 201 Created ao cadastrar registro de energia com sucesso")
    void deveRetornar201AoCriarRegistroDeEnergia() throws Exception {
        LocalDate date = LocalDate.now();
        EnergyRegistryRequestDTO requestDTO = new EnergyRegistryRequestDTO(
                date,
                new BigDecimal("450.75"),
                1L
        );
        EnergyRegistryResponseDTO responseDTO = new EnergyRegistryResponseDTO(
                10L,
                date,
                new BigDecimal("450.75"),
                1L
        );

        when(energyRegistryService.createEnergyRegistry(any(EnergyRegistryRequestDTO.class), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/energy-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/energy-registries/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.energy_consumption").value(450.75))
                .andExpect(jsonPath("$.id_farm").value(1));
    }

    /** Verifica a rejeição de um payload de criação inválido com resposta HTTP 400. */
    @Test
    @DisplayName("POST /energy-registries - Deve retornar 400 Bad Request quando payload for inválido")
    void deveRetornar400AoCriarRegistroComPayloadInvalido() throws Exception {
        EnergyRegistryRequestDTO invalidRequest = new EnergyRegistryRequestDTO(
                null, // data nula
                new BigDecimal("-10.00"), // consumo negativo
                null // id farm nulo
        );

        mockMvc.perform(post("/energy-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    /** Verifica a rejeição de uma data de registro futura com resposta HTTP 400. */
    @Test
    @DisplayName("POST /energy-registries - Deve retornar 400 Bad Request quando data for futura")
    void deveRetornar400AoCriarRegistroComDataFutura() throws Exception {
        EnergyRegistryRequestDTO futureDateRequest = new EnergyRegistryRequestDTO(
                LocalDate.now().plusDays(1),
                new BigDecimal("100.00"),
                1L
        );

        mockMvc.perform(post("/energy-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureDateRequest)))
                .andExpect(status().isBadRequest());
    }

    /** Verifica a listagem de registros de energia com resposta HTTP 200. */
    @Test
    @DisplayName("GET /energy-registries - Deve retornar 200 OK com a lista de registros de energia")
    void deveRetornar200AoListarRegistros() throws Exception {
        EnergyRegistryResponseDTO responseDTO = new EnergyRegistryResponseDTO(
                10L,
                LocalDate.of(2026, 9, 9),
                new BigDecimal("450.75"),
                1L
        );

        when(energyRegistryService.getEnergyRegistriesForUser(eq(null), any(UserPrincipal.class)))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/energy-registries")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].energy_consumption").value(450.75));
    }

    /** Verifica a consulta de um registro de energia por ID com resposta HTTP 200. */
    @Test
    @DisplayName("GET /energy-registries/{id} - Deve retornar 200 OK ao buscar registro por ID")
    void deveRetornar200AoBuscarRegistroPorId() throws Exception {
        EnergyRegistryResponseDTO responseDTO = new EnergyRegistryResponseDTO(
                10L,
                LocalDate.of(2026, 9, 9),
                new BigDecimal("450.75"),
                1L
        );

        when(energyRegistryService.getEnergyRegistryById(eq(10L), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(get("/energy-registries/10")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.energy_consumption").value(450.75));
    }

    /** Verifica a resposta HTTP 404 ao consultar um registro inexistente. */
    @Test
    @DisplayName("GET /energy-registries/{id} - Deve retornar 404 Not Found se registro não for encontrado")
    void deveRetornar404AoBuscarPorIdInexistente() throws Exception {
        when(energyRegistryService.getEnergyRegistryById(eq(99L), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de energia não encontrado"));

        mockMvc.perform(get("/energy-registries/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    /** Verifica a exclusão de um registro de energia com resposta HTTP 204. */
    @Test
    @DisplayName("DELETE /energy-registries/{id} - Deve retornar 204 No Content ao remover registro")
    void deveRetornar204AoDeletarRegistro() throws Exception {
        doNothing().when(energyRegistryService).deleteEnergyRegistry(eq(10L), any(UserPrincipal.class));

        mockMvc.perform(delete("/energy-registries/10")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    /** Verifica a resposta HTTP 401 ao criar um registro sem autenticação. */
    @Test
    @DisplayName("POST /energy-registries - Deve retornar 401 Unauthorized quando requisição não autenticada")
    void deveRetornar401AoCriarRegistroSemAutenticacao() throws Exception {
        mockMvc.perform(post("/energy-registries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    /** Verifica a resposta HTTP 403 ao criar um registro sem permissão. */
    @Test
    @DisplayName("POST /energy-registries - Deve retornar 403 Forbidden quando usuário não tem permissão para a fazenda")
    void deveRetornar403AoCriarRegistroSemPermissao() throws Exception {
        EnergyRegistryRequestDTO requestDTO = new EnergyRegistryRequestDTO(
                LocalDate.of(2026, 9, 9),
                new BigDecimal("450.75"),
                1L
        );

        when(energyRegistryService.createEnergyRegistry(any(EnergyRegistryRequestDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/energy-registries")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    /** Verifica a listagem filtrada pelo identificador da fazenda. */
    @Test
    @DisplayName("GET /energy-registries - Deve retornar 200 OK ao filtrar por farm_id")
    void deveRetornar200AoListarRegistrosComFiltroFarmId() throws Exception {
        EnergyRegistryResponseDTO responseDTO = new EnergyRegistryResponseDTO(
                10L,
                LocalDate.of(2026, 9, 9),
                new BigDecimal("450.75"),
                1L
        );

        when(energyRegistryService.getEnergyRegistriesForUser(eq(1L), any(UserPrincipal.class)))
                .thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/energy-registries")
                        .param("farm_id", "1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].id_farm").value(1));
    }

    /** Verifica a resposta HTTP 403 ao excluir um registro sem permissão. */
    @Test
    @DisplayName("DELETE /energy-registries/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void deveRetornar403AoDeletarRegistroSemPermissao() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"))
                .when(energyRegistryService).deleteEnergyRegistry(eq(10L), any(UserPrincipal.class));

        mockMvc.perform(delete("/energy-registries/10")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }

    /** Verifica a atualização de um registro de energia com resposta HTTP 200. */
    @Test
    @DisplayName("PATCH /energy-registries/{id} - Deve retornar 200 OK ao atualizar registro de energia com sucesso")
    void deveRetornar200AoAtualizarRegistroDeEnergia() throws Exception {
        LocalDate date = LocalDate.now();
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(
                date,
                new BigDecimal("500.00")
        );
        EnergyRegistryResponseDTO responseDTO = new EnergyRegistryResponseDTO(
                10L,
                date,
                new BigDecimal("500.00"),
                1L
        );

        when(energyRegistryService.updateEnergyRegistry(eq(10L), any(EnergyRegistryUpdateDTO.class), any(UserPrincipal.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(patch("/energy-registries/10")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.registration_date").value(date.toString()))
                .andExpect(jsonPath("$.energy_consumption").value(500.00));
    }

    /** Verifica a rejeição de consumo negativo na atualização com resposta HTTP 400. */
    @Test
    @DisplayName("PATCH /energy-registries/{id} - Deve retornar 400 Bad Request quando payload for inválido")
    void deveRetornar400AoAtualizarComConsumoNegativo() throws Exception {
        EnergyRegistryUpdateDTO invalidUpdate = new EnergyRegistryUpdateDTO(
                null,
                new BigDecimal("-50.00")
        );

        mockMvc.perform(patch("/energy-registries/10")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest());
    }

    /** Verifica a rejeição de data futura na atualização com resposta HTTP 400. */
    @Test
    @DisplayName("PATCH /energy-registries/{id} - Deve retornar 400 Bad Request quando data for futura")
    void deveRetornar400AoAtualizarComDataFutura() throws Exception {
        EnergyRegistryUpdateDTO futureDateUpdate = new EnergyRegistryUpdateDTO(
                LocalDate.now().plusDays(1),
                new BigDecimal("500.00")
        );

        mockMvc.perform(patch("/energy-registries/10")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureDateUpdate)))
                .andExpect(status().isBadRequest());
    }

    /** Verifica a resposta HTTP 401 ao atualizar um registro sem autenticação. */
    @Test
    @DisplayName("PATCH /energy-registries/{id} - Deve retornar 401 Unauthorized quando requisição não autenticada")
    void deveRetornar401AoAtualizarSemAutenticacao() throws Exception {
        mockMvc.perform(patch("/energy-registries/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    /** Verifica a resposta HTTP 403 ao atualizar um registro sem permissão. */
    @Test
    @DisplayName("PATCH /energy-registries/{id} - Deve retornar 403 Forbidden quando usuário não tiver permissão")
    void deveRetornar403AoAtualizarSemPermissao() throws Exception {
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(LocalDate.now(), null);

        when(energyRegistryService.updateEnergyRegistry(eq(10L), any(EnergyRegistryUpdateDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(patch("/energy-registries/10")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());
    }

    /** Verifica a resposta HTTP 404 ao atualizar um registro inexistente. */
    @Test
    @DisplayName("PATCH /energy-registries/{id} - Deve retornar 404 Not Found quando registro não for encontrado")
    void deveRetornar404AoAtualizarRegistroInexistente() throws Exception {
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(LocalDate.now(), null);

        when(energyRegistryService.updateEnergyRegistry(eq(99L), any(EnergyRegistryUpdateDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de energia não encontrado"));

        mockMvc.perform(patch("/energy-registries/99")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }
}
