package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.EnterpriseRequestDTO;
import com.ourosapp.springapi.dto.EnterpriseResponseDTO;
import com.ourosapp.springapi.service.EnterpriseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterpriseControllerTest {

    @Mock
    private EnterpriseService enterpriseService;

    @InjectMocks
    private EnterpriseController enterpriseController;

    @Test
    @DisplayName("Deve cadastrar empresa e retornar status 201 Created com cabeçalho Location")
    void testCreateEnterprise() {
        MockHttpServletRequest requestContext = new MockHttpServletRequest();
        requestContext.setServerName("localhost");
        requestContext.setRequestURI("/enterprises");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestContext));

        try {
            EnterpriseRequestDTO request = new EnterpriseRequestDTO(
                    "Agro Ouros S.A.",
                    "contato@agroouros.com.br",
                    "12345678000195",
                    "11999999999",
                    10L
            );
            EnterpriseResponseDTO expectedResponse = new EnterpriseResponseDTO(
                    1L,
                    "Agro Ouros S.A.",
                    "contato@agroouros.com.br",
                    "12345678000195",
                    "11999999999",
                    10L
            );

            when(enterpriseService.createEnterprise(request)).thenReturn(expectedResponse);

            ResponseEntity<EnterpriseResponseDTO> response = enterpriseController.createEnterprise(request);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().id());
            assertEquals("Agro Ouros S.A.", response.getBody().name());
            assertNotNull(response.getHeaders().getLocation());
            assertTrue(response.getHeaders().getLocation().getPath().endsWith("/1"));
            verify(enterpriseService, times(1)).createEnterprise(request);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @DisplayName("Deve retornar lista de empresas com status 200 OK")
    void testGetAllEnterprises() {
        EnterpriseResponseDTO emp1 = new EnterpriseResponseDTO(
                1L,
                "Agro Ouros S.A.",
                "contato@agroouros.com.br",
                "12345678000195",
                "11999999999",
                10L
        );
        when(enterpriseService.getAllEnterprises()).thenReturn(List.of(emp1));

        ResponseEntity<List<EnterpriseResponseDTO>> response = enterpriseController.getAllEnterprises();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Agro Ouros S.A.", response.getBody().get(0).name());
        verify(enterpriseService, times(1)).getAllEnterprises();
    }

    @Test
    @DisplayName("Deve retornar empresa por ID com status 200 OK")
    void testGetEnterpriseById() {
        EnterpriseResponseDTO expectedResponse = new EnterpriseResponseDTO(
                1L,
                "Agro Ouros S.A.",
                "contato@agroouros.com.br",
                "12345678000195",
                "11999999999",
                10L
        );

        when(enterpriseService.getEnterpriseById(1L)).thenReturn(expectedResponse);

        ResponseEntity<EnterpriseResponseDTO> response = enterpriseController.getEnterpriseById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(enterpriseService, times(1)).getEnterpriseById(1L);
    }

    @Test
    @DisplayName("Deve atualizar empresa por ID e retornar status 200 OK")
    void testUpdateEnterprise() {
        EnterpriseRequestDTO request = new EnterpriseRequestDTO(
                "Agro Ouros Renovada S.A.",
                "novo@agroouros.com.br",
                "12345678000195",
                "11988887777",
                10L
        );
        EnterpriseResponseDTO expectedResponse = new EnterpriseResponseDTO(
                1L,
                "Agro Ouros Renovada S.A.",
                "novo@agroouros.com.br",
                "12345678000195",
                "11988887777",
                10L
        );

        when(enterpriseService.updateEnterprise(1L, request)).thenReturn(expectedResponse);

        ResponseEntity<EnterpriseResponseDTO> response = enterpriseController.updateEnterprise(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("Agro Ouros Renovada S.A.", response.getBody().name());
        verify(enterpriseService, times(1)).updateEnterprise(1L, request);
    }
}
