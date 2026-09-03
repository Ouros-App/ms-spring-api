package com.ourosapp.springapi.controller;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.CompanyEmployeeService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração e contrato HTTP para {@link CompanyEmployeeController} utilizando MockMvc.
 */
@WebMvcTest(CompanyEmployeeController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class CompanyEmployeeControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyEmployeeService companyEmployeeService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    /**
     * Testa POST /company-employees esperando status 201 Created e cabeçalho Location.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("POST /company-employees - Deve cadastrar funcionário e retornar 201 Created com cabeçalho Location")
    void testCreateCompanyEmployeeSuccess() throws Exception {
        CompanyEmployeeRequestDTO request = new CompanyEmployeeRequestDTO(
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                "SenhaForte@123",
                10L
        );
        CompanyEmployeeResponseDTO response = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                10L
        );

        when(companyEmployeeService.createCompanyEmployee(any(CompanyEmployeeRequestDTO.class), any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(post("/company-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/company-employees/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Carlos Eduardo Pereira"))
                .andExpect(jsonPath("$.document_number").value("12345678909"))
                .andExpect(jsonPath("$.email").value("carlos.pereira@empresa.com.br"))
                .andExpect(jsonPath("$.telephone").value("11987654321"))
                .andExpect(jsonPath("$.id_enterprise").value(10L));
    }

    /**
     * Testa POST /company-employees sem autenticação esperando status 401 Unauthorized.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @DisplayName("POST /company-employees - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreateCompanyEmployeeUnauthorized() throws Exception {
        CompanyEmployeeRequestDTO request = new CompanyEmployeeRequestDTO(
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                "SenhaForte@123",
                10L
        );

        mockMvc.perform(post("/company-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Testa POST /company-employees com payload inválido esperando status 400 Bad Request.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("POST /company-employees - Deve retornar 400 Bad Request quando payload for inválido")
    void testCreateCompanyEmployeeInvalidPayload() throws Exception {
        CompanyEmployeeRequestDTO invalidRequest = new CompanyEmployeeRequestDTO(
                "",
                "123",
                "email-invalido",
                "123",
                "123",
                null
        );

        mockMvc.perform(post("/company-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Testa GET /company-employees/me com usuário autenticado esperando status 200 OK.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @DisplayName("GET /company-employees/me - Deve retornar 200 OK com dados do usuário autenticado")
    void testGetLoggedInEmployeeSuccess() throws Exception {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "carlos.pereira@empresa.com.br",
                null,
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        CompanyEmployeeResponseDTO response = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                10L
        );

        when(companyEmployeeService.getLoggedInEmployee(any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(get("/company-employees/me")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Carlos Eduardo Pereira"))
                .andExpect(jsonPath("$.email").value("carlos.pereira@empresa.com.br"));
    }

    /**
     * Testa GET /company-employees/me sem autenticação esperando status 401 Unauthorized.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @DisplayName("GET /company-employees/me - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetLoggedInEmployeeUnauthorized() throws Exception {
        mockMvc.perform(get("/company-employees/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Testa GET /company-employees/{id} quando o funcionário existe esperando status 200 OK.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("GET /company-employees/{id} - Deve retornar 200 OK quando funcionário existir")
    void testGetCompanyEmployeeByIdSuccess() throws Exception {
        CompanyEmployeeResponseDTO response = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                10L
        );

        when(companyEmployeeService.getCompanyEmployeeById(1L, any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(get("/company-employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Carlos Eduardo Pereira"))
                .andExpect(jsonPath("$.document_number").value("12345678909"));
    }

    /**
     * Testa GET /company-employees/{id} quando o funcionário não existe esperando status 404 Not Found.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("GET /company-employees/{id} - Deve retornar 404 Not Found quando funcionário não existir")
    void testGetCompanyEmployeeByIdNotFound() throws Exception {
        when(companyEmployeeService.getCompanyEmployeeById(99L, any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));

        mockMvc.perform(get("/company-employees/99"))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa PATCH /company-employees/{id} com dados válidos esperando status 200 OK.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("PATCH /company-employees/{id} - Deve atualizar parcialmente e retornar 200 OK")
    void testUpdateCompanyEmployeeSuccess() throws Exception {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "carlos.novo@empresa.com.br",
                "11999998888",
                "NovaSenha@123"
        );
        CompanyEmployeeResponseDTO response = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.novo@empresa.com.br",
                "11999998888",
                10L
        );

        when(companyEmployeeService.updateCompanyEmployee(eq(1L), any(CompanyEmployeeUpdateDTO.class), any(UserPrincipal.class))).thenReturn(response);

        mockMvc.perform(patch("/company-employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("carlos.novo@empresa.com.br"))
                .andExpect(jsonPath("$.telephone").value("11999998888"));
    }

    /**
     * Testa PATCH /company-employees/{id} para funcionário inexistente esperando status 404 Not Found.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("PATCH /company-employees/{id} - Deve retornar 404 Not Found quando funcionário não existir")
    void testUpdateCompanyEmployeeNotFound() throws Exception {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "carlos.novo@empresa.com.br",
                null,
                null
        );

        when(companyEmployeeService.updateCompanyEmployee(eq(99L), any(CompanyEmployeeUpdateDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));

        mockMvc.perform(patch("/company-employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    /**
     * Testa PATCH /company-employees/{id} com e-mail duplicado de outro funcionário esperando status 409 Conflict.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("PATCH /company-employees/{id} - Deve retornar 409 Conflict quando e-mail já pertencer a outro funcionário")
    void testUpdateCompanyEmployeeConflict() throws Exception {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "duplicado@empresa.com.br",
                null,
                null
        );

        when(companyEmployeeService.updateCompanyEmployee(eq(1L), any(CompanyEmployeeUpdateDTO.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outro funcionário cadastrado com este e-mail"));

        mockMvc.perform(patch("/company-employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isConflict());
    }

    /**
     * Testa PATCH /company-employees/{id} com senha fora do padrão de complexidade esperando status 400 Bad Request.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("PATCH /company-employees/{id} - Deve retornar 400 Bad Request quando senha não cumprir requisitos de complexidade")
    void testUpdateCompanyEmployeeInvalidPassword() throws Exception {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                null,
                null,
                "senha123"
        );

        mockMvc.perform(patch("/company-employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Testa DELETE /company-employees/{id} para remoção bem-sucedida esperando status 204 No Content.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("DELETE /company-employees/{id} - Deve remover funcionário e retornar 204 No Content")
    void testDeleteCompanyEmployeeSuccess() throws Exception {
        doNothing().when(companyEmployeeService).deleteCompanyEmployee(1L, any(UserPrincipal.class));

        mockMvc.perform(delete("/company-employees/1"))
                .andExpect(status().isNoContent());
    }

    /**
     * Testa DELETE /company-employees/{id} quando funcionário não existe esperando status 404 Not Found.
     *
     * @throws Exception se ocorrer erro na requisição MockMvc
     */
    @Test
    @WithMockUser
    @DisplayName("DELETE /company-employees/{id} - Deve retornar 404 Not Found quando funcionário não existir")
    void testDeleteCompanyEmployeeNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"))
                .when(companyEmployeeService).deleteCompanyEmployee(99L, any(UserPrincipal.class));

        mockMvc.perform(delete("/company-employees/99"))
                .andExpect(status().isNotFound());
    }
}
