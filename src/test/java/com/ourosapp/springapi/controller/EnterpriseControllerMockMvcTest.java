package com.ourosapp.springapi.controller;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.service.EnterpriseService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnterpriseController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class EnterpriseControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnterpriseService enterpriseService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("POST /enterprises - Deve criar empresa e retornar 201 Created com cabeçalho Location")
    void testCreateEnterpriseSuccess() throws Exception {
        EnterpriseRequestDTO request = new EnterpriseRequestDTO("Agro Ouros S.A.", "contato@agroouros.com.br", "12345678000195", "11999999999", 1L
        , new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR"));
        EnterpriseResponseDTO response = new EnterpriseResponseDTO(
                1L,
                "Agro Ouros S.A.",
                "contato@agroouros.com.br",
                "12345678000195",
                "11999999999",
                1L
        );

        when(enterpriseService.createEnterprise(any(EnterpriseRequestDTO.class), any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(post("/enterprises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/enterprises/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Agro Ouros S.A."))
                .andExpect(jsonPath("$.email").value("contato@agroouros.com.br"))
                .andExpect(jsonPath("$.document_number").value("12345678000195"))
                .andExpect(jsonPath("$.telephone").value("11999999999"))
                .andExpect(jsonPath("$.id_address").value(1L));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /enterprises - Deve aceitar payload em formato camelCase (interoperabilidade)")
    void testCreateEnterpriseCamelCasePayloadSuccess() throws Exception {
        String camelCasePayload = """
                {
                    "name": "Agro Ouros S.A.",
                    "email": "contato@agroouros.com.br",
                    "documentNumber": "12345678000195",
                    "telephone": "11999999999",
                    "idAddress": 1
                }
                """;
        EnterpriseResponseDTO response = new EnterpriseResponseDTO(
                1L,
                "Agro Ouros S.A.",
                "contato@agroouros.com.br",
                "12345678000195",
                "11999999999",
                1L
        );

        when(enterpriseService.createEnterprise(any(EnterpriseRequestDTO.class), any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(post("/enterprises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCasePayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/enterprises/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Agro Ouros S.A."));
    }

    @Test
    @DisplayName("POST /enterprises - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateEnterpriseUnauthorized() throws Exception {
        EnterpriseRequestDTO request = new EnterpriseRequestDTO("Agro Ouros S.A.", "contato@agroouros.com.br", "12345678000195", "11999999999", 1L
        , new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR"));

        mockMvc.perform(post("/enterprises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /enterprises - Deve retornar 400 Bad Request quando campos obrigatórios forem inválidos")
    void testCreateEnterpriseInvalidPayload() throws Exception {
        EnterpriseRequestDTO invalidRequest = new EnterpriseRequestDTO("", "email-invalido", "123", "123", null
        , new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR"));

        mockMvc.perform(post("/enterprises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /enterprises - Deve retornar 200 OK e lista de empresas cadastradas")
    void testGetAllEnterprisesSuccess() throws Exception {
        EnterpriseResponseDTO response = new EnterpriseResponseDTO(
                1L,
                "Agro Ouros S.A.",
                "contato@agroouros.com.br",
                "12345678000195",
                "11999999999",
                1L
        );

        when(enterpriseService.getAllEnterprises(any(UserPrincipal.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/enterprises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Agro Ouros S.A."));
    }

    @Test
    @DisplayName("GET /enterprises - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetAllEnterprisesUnauthorized() throws Exception {
        mockMvc.perform(get("/enterprises"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /enterprises/{id} - Deve retornar 200 OK e dados da empresa quando encontrada")
    void testGetEnterpriseByIdSuccess() throws Exception {
        EnterpriseResponseDTO response = new EnterpriseResponseDTO(
                1L,
                "Agro Ouros S.A.",
                "contato@agroouros.com.br",
                "12345678000195",
                "11999999999",
                1L
        );

        when(enterpriseService.getEnterpriseById(1L, any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(get("/enterprises/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Agro Ouros S.A."))
                .andExpect(jsonPath("$.document_number").value("12345678000195"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /enterprises/{id} - Deve retornar 404 Not Found quando empresa não existir")
    void testGetEnterpriseByIdNotFound() throws Exception {
        when(enterpriseService.getEnterpriseById(99L, any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada"));

        mockMvc.perform(get("/enterprises/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /enterprises/{id} - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetEnterpriseByIdUnauthorized() throws Exception {
        mockMvc.perform(get("/enterprises/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /enterprises/{id} - Deve atualizar empresa e retornar 200 OK quando válido")
    void testUpdateEnterpriseSuccess() throws Exception {
        EnterpriseUpdateDTO request = new EnterpriseUpdateDTO("Agro Ouros Renovada S.A.", "novo@agroouros.com.br", "12345678000195", "11988887777", 1L);
        EnterpriseResponseDTO response = new EnterpriseResponseDTO(
                1L,
                "Agro Ouros Renovada S.A.",
                "novo@agroouros.com.br",
                "12345678000195",
                "11988887777",
                1L
        );

        when(enterpriseService.updateEnterprise(eq(1L), any(EnterpriseUpdateDTO.class), any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(patch("/enterprises/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Agro Ouros Renovada S.A."))
                .andExpect(jsonPath("$.email").value("novo@agroouros.com.br"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /enterprises/{id} - Deve retornar 404 Not Found ao tentar atualizar empresa inexistente")
    void testUpdateEnterpriseNotFound() throws Exception {
        EnterpriseUpdateDTO request = new EnterpriseUpdateDTO("Agro Ouros Renovada S.A.", "novo@agroouros.com.br", "12345678000195", "11988887777", 1L);

        when(enterpriseService.updateEnterprise(eq(99L), any(EnterpriseUpdateDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada"));

        mockMvc.perform(patch("/enterprises/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /enterprises/{id} - Deve retornar 400 Bad Request quando payload for inválido")
    void testUpdateEnterpriseInvalidPayload() throws Exception {
        EnterpriseUpdateDTO invalidRequest = new EnterpriseUpdateDTO("", "email-invalido", "123", "123", null);

        mockMvc.perform(patch("/enterprises/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /enterprises - Deve retornar 409 Conflict quando empresa com mesmo CNPJ ou e-mail já existir")
    void testCreateEnterpriseConflict() throws Exception {
        EnterpriseRequestDTO request = new EnterpriseRequestDTO("Agro Ouros S.A.", "contato@agroouros.com.br", "12345678000195", "11999999999", 1L
        , new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR"));

        when(enterpriseService.createEnterprise(any(EnterpriseRequestDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma empresa cadastrada com este CNPJ"));

        mockMvc.perform(post("/enterprises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /enterprises/{id} - Deve retornar 409 Conflict quando CNPJ ou e-mail pertencer a outra empresa")
    void testUpdateEnterpriseConflict() throws Exception {
        EnterpriseUpdateDTO request = new EnterpriseUpdateDTO("Agro Ouros Renovada S.A.", "novo@agroouros.com.br", "12345678000195", "11988887777", 1L);

        when(enterpriseService.updateEnterprise(eq(1L), any(EnterpriseUpdateDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outra empresa cadastrada com este CNPJ"));

        mockMvc.perform(patch("/enterprises/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /enterprises/{id} - Deve retornar 401 Unauthorized quando não autenticado")
    void testUpdateEnterpriseUnauthorized() throws Exception {
        EnterpriseUpdateDTO request = new EnterpriseUpdateDTO("Agro Ouros Renovada S.A.", "novo@agroouros.com.br", "12345678000195", "11988887777", 1L);

        mockMvc.perform(patch("/enterprises/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
