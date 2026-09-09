package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerRequestDTO;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerResponseDTO;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerUpdateDTO;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmOwnerService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração e contrato HTTP para {@link FarmOwnerController} utilizando MockMvc.
 */
@WebMvcTest(FarmOwnerController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class FarmOwnerControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FarmOwnerService farmOwnerService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    /**
     * Testa POST /farm-owners esperando status 201 Created e cabeçalho Location.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("POST /farm-owners - Deve cadastrar produtor rural e retornar 201 Created com cabeçalho Location")
    void testCreateFarmOwnerSuccess() throws Exception {
        FarmOwnerRequestDTO request = new FarmOwnerRequestDTO(
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.silva@fazenda.com.br",
                "11987654321",
                "SenhaForte@123",
                10L
        );
        FarmOwnerResponseDTO response = new FarmOwnerResponseDTO(
                1L,
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.silva@fazenda.com.br",
                "11987654321",
                10L
        );

        when(farmOwnerService.createFarmOwner(any(FarmOwnerRequestDTO.class), any())).thenReturn(response);

        mockMvc.perform(post("/farm-owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/farm-owners/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Sebastião da Silva"))
                .andExpect(jsonPath("$.document_number").value("12345678909"))
                .andExpect(jsonPath("$.email").value("sebastiao.silva@fazenda.com.br"))
                .andExpect(jsonPath("$.telephone").value("11987654321"))
                .andExpect(jsonPath("$.id_farm").value(10L));
    }

    /**
     * Testa POST /farm-owners sem autenticação esperando status 401 Unauthorized.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @DisplayName("POST /farm-owners - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateFarmOwnerUnauthorized() throws Exception {
        FarmOwnerRequestDTO request = new FarmOwnerRequestDTO(
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.silva@fazenda.com.br",
                "11987654321",
                "SenhaForte@123",
                10L
        );

        mockMvc.perform(post("/farm-owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Testa POST /farm-owners com payload inválido esperando status 400 Bad Request.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("POST /farm-owners - Deve retornar 400 Bad Request quando payload for inválido")
    void testCreateFarmOwnerInvalidPayload() throws Exception {
        FarmOwnerRequestDTO invalidRequest = new FarmOwnerRequestDTO(
                "",
                "123",
                "email-invalido",
                "123",
                "123",
                null
        );

        mockMvc.perform(post("/farm-owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Testa GET /farm-owners/me com produtor logado esperando status 200 OK.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @DisplayName("GET /farm-owners/me - Deve retornar 200 OK com dados do produtor autenticado")
    void testGetLoggedInFarmOwnerSuccess() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "sebastiao.silva@fazenda.com.br",
                null,
                "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );

        FarmOwnerResponseDTO response = new FarmOwnerResponseDTO(
                1L,
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.silva@fazenda.com.br",
                "11987654321",
                10L
        );

        when(farmOwnerService.getLoggedInFarmOwner(any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(get("/farm-owners/me")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Sebastião da Silva"))
                .andExpect(jsonPath("$.document_number").value("12345678909"))
                .andExpect(jsonPath("$.email").value("sebastiao.silva@fazenda.com.br"))
                .andExpect(jsonPath("$.id_farm").value(10L));
    }

    /**
     * Testa GET /farm-owners/me sem autenticação esperando status 401 Unauthorized.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @DisplayName("GET /farm-owners/me - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetLoggedInFarmOwnerUnauthorized() throws Exception {
        mockMvc.perform(get("/farm-owners/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Testa GET /farm-owners (listagem) com usuário autenticado esperando status 200 OK e lista de produtores.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("GET /farm-owners - Deve listar produtores rurais e retornar 200 OK")
    void testGetFarmOwnersSuccess() throws Exception {
        FarmOwnerResponseDTO response = new FarmOwnerResponseDTO(
                1L,
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.silva@fazenda.com.br",
                "11987654321",
                10L
        );

        when(farmOwnerService.getFarmOwners(eq(null), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/farm-owners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Sebastião da Silva"))
                .andExpect(jsonPath("$[0].document_number").value("12345678909"));
    }

    /**
     * Testa GET /farm-owners com query param farmId esperando status 200 OK.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("GET /farm-owners?farmId=10 - Deve listar produtores filtrados por fazenda")
    void testGetFarmOwnersWithFarmFilterSuccess() throws Exception {
        FarmOwnerResponseDTO response = new FarmOwnerResponseDTO(
                1L,
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.silva@fazenda.com.br",
                "11987654321",
                10L
        );

        when(farmOwnerService.getFarmOwners(eq(10L), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/farm-owners").param("farmId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].id_farm").value(10L));
    }

    /**
     * Testa GET /farm-owners/{id} quando produtor existe esperando status 200 OK.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("GET /farm-owners/{id} - Deve retornar 200 OK quando produtor existir")
    void testGetFarmOwnerByIdSuccess() throws Exception {
        FarmOwnerResponseDTO response = new FarmOwnerResponseDTO(
                1L,
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.silva@fazenda.com.br",
                "11987654321",
                10L
        );

        when(farmOwnerService.getFarmOwnerById(eq(1L), any())).thenReturn(response);

        mockMvc.perform(get("/farm-owners/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Sebastião da Silva"))
                .andExpect(jsonPath("$.document_number").value("12345678909"));
    }

    /**
     * Testa GET /farm-owners/{id} quando produtor não existe esperando status 404 Not Found.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("GET /farm-owners/{id} - Deve retornar 404 Not Found quando produtor não existir")
    void testGetFarmOwnerByIdNotFound() throws Exception {
        when(farmOwnerService.getFarmOwnerById(eq(99L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural não encontrado"));

        mockMvc.perform(get("/farm-owners/99"))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa PATCH /farm-owners/{id} com dados válidos esperando status 200 OK.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("PATCH /farm-owners/{id} - Deve atualizar parcialmente e retornar 200 OK")
    void testUpdateFarmOwnerSuccess() throws Exception {
        FarmOwnerUpdateDTO updateDTO = new FarmOwnerUpdateDTO(
                "sebastiao.novo@fazenda.com.br",
                "11999998888",
                "NovaSenha@123"
        );
        FarmOwnerResponseDTO response = new FarmOwnerResponseDTO(
                1L,
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.novo@fazenda.com.br",
                "11999998888",
                10L
        );

        when(farmOwnerService.updateFarmOwner(eq(1L), any(FarmOwnerUpdateDTO.class), any())).thenReturn(response);

        mockMvc.perform(patch("/farm-owners/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("sebastiao.novo@fazenda.com.br"))
                .andExpect(jsonPath("$.telephone").value("11999998888"));
    }

    /**
     * Testa PATCH /farm-owners/{id} para produtor inexistente esperando status 404 Not Found.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("PATCH /farm-owners/{id} - Deve retornar 404 Not Found quando produtor não existir")
    void testUpdateFarmOwnerNotFound() throws Exception {
        FarmOwnerUpdateDTO updateDTO = new FarmOwnerUpdateDTO(
                "sebastiao.novo@fazenda.com.br",
                null,
                null
        );

        when(farmOwnerService.updateFarmOwner(eq(99L), any(FarmOwnerUpdateDTO.class), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural não encontrado"));

        mockMvc.perform(patch("/farm-owners/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa PATCH /farm-owners/{id} com e-mail duplicado de outro produtor esperando status 409 Conflict.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("PATCH /farm-owners/{id} - Deve retornar 409 Conflict quando e-mail pertencer a outro produtor")
    void testUpdateFarmOwnerConflict() throws Exception {
        FarmOwnerUpdateDTO updateDTO = new FarmOwnerUpdateDTO(
                "duplicado@fazenda.com.br",
                null,
                null
        );

        when(farmOwnerService.updateFarmOwner(eq(1L), any(FarmOwnerUpdateDTO.class), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outro produtor rural cadastrado com este e-mail"));

        mockMvc.perform(patch("/farm-owners/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isConflict());
    }

    /**
     * Testa DELETE /farm-owners/{id} com sucesso esperando status 204 No Content.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("DELETE /farm-owners/{id} - Deve remover produtor rural e retornar 204 No Content")
    void testDeleteFarmOwnerSuccess() throws Exception {
        doNothing().when(farmOwnerService).deleteFarmOwner(eq(1L), any());

        mockMvc.perform(delete("/farm-owners/1"))
                .andExpect(status().isNoContent());
    }

    /**
     * Testa DELETE /farm-owners/{id} quando produtor não existe esperando status 404 Not Found.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("DELETE /farm-owners/{id} - Deve retornar 404 Not Found quando produtor não existir")
    void testDeleteFarmOwnerNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural não encontrado"))
                .when(farmOwnerService).deleteFarmOwner(eq(99L), any());

        mockMvc.perform(delete("/farm-owners/99"))
                .andExpect(status().isNotFound());
    }
}
