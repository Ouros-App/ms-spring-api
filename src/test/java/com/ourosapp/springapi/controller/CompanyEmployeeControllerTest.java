package com.ourosapp.springapi.controller;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.CompanyEmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o controlador {@link CompanyEmployeeController}.
 */
@ExtendWith(MockitoExtension.class)
class CompanyEmployeeControllerTest {

    @Mock
    private CompanyEmployeeService companyEmployeeService;

    @InjectMocks
    private CompanyEmployeeController companyEmployeeController;

    /**
     * Testa o cadastro bem-sucedido de funcionário, verificando o status 201 Created e o header Location.
     */
    @Test
    @DisplayName("Deve cadastrar funcionário e retornar status 201 Created com cabeçalho Location")
    void testCreateCompanyEmployee() {
        MockHttpServletRequest requestContext = new MockHttpServletRequest();
        requestContext.setServerName("localhost");
        requestContext.setRequestURI("/company-employees");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestContext));

        try {
            CompanyEmployeeRequestDTO request = new CompanyEmployeeRequestDTO(
                    "Carlos Eduardo Pereira",
                    "12345678901",
                    "carlos.pereira@empresa.com.br",
                    "11987654321",
                    "SenhaForte@123",
                    10L
            );
            CompanyEmployeeResponseDTO expectedResponse = new CompanyEmployeeResponseDTO(
                    1L,
                    "Carlos Eduardo Pereira",
                    "12345678901",
                    "carlos.pereira@empresa.com.br",
                    "11987654321",
                    10L
            );

            when(companyEmployeeService.createCompanyEmployee(request, any(UserPrincipal.class))).thenReturn(expectedResponse);

            ResponseEntity<CompanyEmployeeResponseDTO> response = companyEmployeeController.createCompanyEmployee(request, mock(UserPrincipal.class));

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().id());
            assertEquals("Carlos Eduardo Pereira", response.getBody().name());
            assertNotNull(response.getHeaders().getLocation());
            assertTrue(response.getHeaders().getLocation().getPath().endsWith("/1"));
            verify(companyEmployeeService, times(1)).createCompanyEmployee(request, any(UserPrincipal.class));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * Testa a recuperação dos dados do funcionário logado a partir do UserPrincipal com status 200 OK.
     */
    @Test
    @DisplayName("Deve retornar dados do funcionário logado com status 200 OK")
    void testGetLoggedInEmployee() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "carlos.pereira@empresa.com.br",
                null,
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        CompanyEmployeeResponseDTO expectedResponse = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678901",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                10L
        );

        when(companyEmployeeService.getLoggedInEmployee(principal)).thenReturn(expectedResponse);

        ResponseEntity<CompanyEmployeeResponseDTO> response = companyEmployeeController.getLoggedInEmployee(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(companyEmployeeService, times(1)).getLoggedInEmployee(principal);
    }

    /**
     * Testa a busca de funcionário por ID com status 200 OK.
     */
    @Test
    @DisplayName("Deve retornar funcionário por ID com status 200 OK")
    void testGetCompanyEmployeeById() {
        CompanyEmployeeResponseDTO expectedResponse = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678901",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                10L
        );

        when(companyEmployeeService.getCompanyEmployeeById(1L, any(UserPrincipal.class))).thenReturn(expectedResponse);

        ResponseEntity<CompanyEmployeeResponseDTO> response = companyEmployeeController.getCompanyEmployeeById(1L, mock(UserPrincipal.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(companyEmployeeService, times(1)).getCompanyEmployeeById(1L, any(UserPrincipal.class));
    }

    /**
     * Testa a atualização parcial de funcionário com status 200 OK.
     */
    @Test
    @DisplayName("Deve atualizar funcionário por ID e retornar status 200 OK")
    void testUpdateCompanyEmployee() {
        CompanyEmployeeUpdateDTO request = new CompanyEmployeeUpdateDTO(
                "carlos.novo@empresa.com.br",
                "11999998888",
                "NovaSenha@123"
        );
        CompanyEmployeeResponseDTO expectedResponse = new CompanyEmployeeResponseDTO(
                1L,
                "Carlos Eduardo Pereira",
                "12345678901",
                "carlos.novo@empresa.com.br",
                "11999998888",
                10L
        );

        when(companyEmployeeService.updateCompanyEmployee(1L, request, any(UserPrincipal.class))).thenReturn(expectedResponse);

        ResponseEntity<CompanyEmployeeResponseDTO> response = companyEmployeeController.updateCompanyEmployee(1L, request, mock(UserPrincipal.class));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("carlos.novo@empresa.com.br", response.getBody().email());
        assertEquals("11999998888", response.getBody().telephone());
        verify(companyEmployeeService, times(1)).updateCompanyEmployee(1L, request, any(UserPrincipal.class));
    }

    /**
     * Testa a remoção de funcionário por ID com status 204 No Content.
     */
    @Test
    @DisplayName("Deve excluir funcionário por ID e retornar status 204 No Content")
    void testDeleteCompanyEmployee() {
        doNothing().when(companyEmployeeService).deleteCompanyEmployee(1L, any(UserPrincipal.class));

        ResponseEntity<Void> response = companyEmployeeController.deleteCompanyEmployee(1L, mock(UserPrincipal.class));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(companyEmployeeService, times(1)).deleteCompanyEmployee(1L, any(UserPrincipal.class));
    }
}
