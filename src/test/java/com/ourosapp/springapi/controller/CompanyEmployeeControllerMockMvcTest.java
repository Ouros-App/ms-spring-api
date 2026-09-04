package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeRequestDTO;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeResponseDTO;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeUpdateDTO;
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
import static org.mockito.Mockito.doThrow;
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

        when(companyEmployeeService.createCompanyEmployee(any(), any())).thenReturn(response);

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

    @Test
    @WithMockUser
    @DisplayName("POST /company-employees - Deve aceitar payload em formato camelCase (interoperabilidade)")
    void testCreateCompanyEmployeeCamelCasePayloadSuccess() throws Exception {
        String camelCasePayload = """
                {
                    "name": "Carlos Eduardo Pereira",
                    "documentNumber": "12345678909",
                    "email": "carlos.pereira@empresa.com.br",
                    "telephone": "11987654321",
                    "password": "SenhaForte@123",
                    "idEnterprise": 10
                }
                """;
        CompanyEmployeeResponseDTO response = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                10L
        );

        when(companyEmployeeService.createCompanyEmployee(any(), any())).thenReturn(response);

        mockMvc.perform(post("/company-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCasePayload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/company-employees/1")))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Carlos Eduardo Pereira"));
    }

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

    @Test
    @WithMockUser
    @DisplayName("POST /company-employees - Deve retornar 400 Bad Request quando campos obrigatórios forem inválidos")
    void testCreateCompanyEmployeeInvalidPayload() throws Exception {
        CompanyEmployeeRequestDTO invalidRequest = new CompanyEmployeeRequestDTO(
                "",
                "123",
                "email-invalido",
                "123",
                "fraca",
                null
        );

        mockMvc.perform(post("/company-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /company-employees/me - Deve retornar 200 OK e dados do funcionário logado")
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

        when(companyEmployeeService.getLoggedInEmployee(any())).thenReturn(response);

        mockMvc.perform(get("/company-employees/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Carlos Eduardo Pereira"))
                .andExpect(jsonPath("$.email").value("carlos.pereira@empresa.com.br"));
    }

    @Test
    @DisplayName("GET /company-employees/me - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetLoggedInEmployeeUnauthorized() throws Exception {
        mockMvc.perform(get("/company-employees/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /company-employees/me - Deve retornar 403 Forbidden quando perfil não for COMPANY_EMPLOYEE")
    void testGetLoggedInEmployeeForbiddenForNonEmployee() throws Exception {
        UserPrincipal admPrincipal = new UserPrincipal(
                1L,
                "adm@empresa.com.br",
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        when(companyEmployeeService.getLoggedInEmployee(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a funcionários"));

        mockMvc.perform(get("/company-employees/me").with(user(admPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /company-employees/{id} - Deve retornar 200 OK e dados do funcionário quando encontrado")
    void testGetCompanyEmployeeByIdSuccess() throws Exception {
        CompanyEmployeeResponseDTO response = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                10L
        );

        when(companyEmployeeService.getCompanyEmployeeById(eq(1L), any())).thenReturn(response);

        mockMvc.perform(get("/company-employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Carlos Eduardo Pereira"))
                .andExpect(jsonPath("$.document_number").value("12345678909"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /company-employees/{id} - Deve retornar 404 Not Found quando funcionário não existir")
    void testGetCompanyEmployeeByIdNotFound() throws Exception {
        when(companyEmployeeService.getCompanyEmployeeById(eq(99L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));

        mockMvc.perform(get("/company-employees/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /company-employees/{id} - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetCompanyEmployeeByIdUnauthorized() throws Exception {
        mockMvc.perform(get("/company-employees/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /company-employees/{id} - Deve atualizar funcionário e retornar 200 OK quando válido")
    void testUpdateCompanyEmployeeSuccess() throws Exception {
        CompanyEmployeeUpdateDTO request = new CompanyEmployeeUpdateDTO(
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

        when(companyEmployeeService.updateCompanyEmployee(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/company-employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("carlos.novo@empresa.com.br"))
                .andExpect(jsonPath("$.telephone").value("11999998888"));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /company-employees/{id} - Deve retornar 404 Not Found ao tentar atualizar funcionário inexistente")
    void testUpdateCompanyEmployeeNotFound() throws Exception {
        CompanyEmployeeUpdateDTO request = new CompanyEmployeeUpdateDTO(
                "carlos.novo@empresa.com.br",
                "11999998888",
                "NovaSenha@123"
        );

        when(companyEmployeeService.updateCompanyEmployee(eq(99L), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));

        mockMvc.perform(patch("/company-employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /company-employees/{id} - Deve retornar 400 Bad Request quando payload for inválido")
    void testUpdateCompanyEmployeeInvalidPayload() throws Exception {
        CompanyEmployeeUpdateDTO invalidRequest = new CompanyEmployeeUpdateDTO(
                "email-invalido",
                "123",
                "fraca"
        );

        mockMvc.perform(patch("/company-employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /company-employees/{id} - Deve excluir funcionário e retornar 204 No Content quando existir")
    void testDeleteCompanyEmployeeSuccess() throws Exception {
        doNothing().when(companyEmployeeService).deleteCompanyEmployee(eq(1L), any());

        mockMvc.perform(delete("/company-employees/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /company-employees/{id} - Deve retornar 404 Not Found ao tentar excluir funcionário inexistente")
    void testDeleteCompanyEmployeeNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"))
                .when(companyEmployeeService).deleteCompanyEmployee(eq(99L), any());

        mockMvc.perform(delete("/company-employees/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /company-employees/{id} - Deve retornar 401 Unauthorized quando não autenticado")
    void testDeleteCompanyEmployeeUnauthorized() throws Exception {
        mockMvc.perform(delete("/company-employees/1"))
                .andExpect(status().isUnauthorized());
    }
}
