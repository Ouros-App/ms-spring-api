package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.farm.FarmRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o controlador REST {@link FarmController}.
 */
@ExtendWith(MockitoExtension.class)
class FarmControllerTest {

    @Mock
    private FarmService farmService;

    @InjectMocks
    private FarmController farmController;

    /**
     * Testa o cadastro de fazenda no controller esperando status 201 Created e cabeçalho Location.
     */
    @Test
    @DisplayName("Deve cadastrar fazenda e retornar status 201 Created com cabeçalho Location")
    void testCreateFarm() {
        MockHttpServletRequest requestContext = new MockHttpServletRequest();
        requestContext.setServerName("localhost");
        requestContext.setRequestURI("/farms");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestContext));

        try {
            UserPrincipal principal = new UserPrincipal(
                    1L,
                    "adm@ouros.com",
                    null,
                    "ADM",
                    List.of(new SimpleGrantedAuthority("ROLE_ADM"))
            );
            FarmRequestDTO request = new FarmRequestDTO(
                    "Fazenda Ouro Verde",
                    new BigDecimal("150.50"),
                    "Sudeste",
                    50000,
                    "Gleba 4 - Setor Sul",
                    10L,
                    null,
                    20L
            );
            FarmResponseDTO expectedResponse = new FarmResponseDTO(
                    1L,
                    "Fazenda Ouro Verde",
                    new BigDecimal("150.50"),
                    "Sudeste",
                    50000,
                    "Gleba 4 - Setor Sul",
                    10L,
                    20L
            );

            when(farmService.createFarm(request, principal)).thenReturn(expectedResponse);

            ResponseEntity<FarmResponseDTO> response = farmController.createFarm(request, principal);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().id());
            assertEquals("Fazenda Ouro Verde", response.getBody().name());
            assertNotNull(response.getHeaders().getLocation());
            assertTrue(response.getHeaders().getLocation().getPath().endsWith("/1"));
            verify(farmService, times(1)).createFarm(request, principal);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * Testa listagem de fazendas do usuário logado esperando status 200 OK.
     */
    @Test
    @DisplayName("Deve retornar lista de fazendas vinculadas ao usuário com status 200 OK")
    void testGetFarmsForUser() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "usuario@empresa.com",
                null,
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        FarmResponseDTO farm1 = new FarmResponseDTO(
                1L,
                "Fazenda Ouro Verde",
                new BigDecimal("150.50"),
                "Sudeste",
                50000,
                "Gleba 4 - Setor Sul",
                10L,
                20L
        );

        when(farmService.getFarmsForUser(principal)).thenReturn(List.of(farm1));

        ResponseEntity<List<FarmResponseDTO>> response = farmController.getFarmsForUser(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Fazenda Ouro Verde", response.getBody().get(0).name());
        verify(farmService, times(1)).getFarmsForUser(principal);
    }

    /**
     * Testa busca de fazenda por ID no controller esperando status 200 OK.
     */
    @Test
    @DisplayName("Deve buscar fazenda por ID e retornar status 200 OK")
    void testGetFarmById() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "adm@ouros.com",
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
            );
        FarmResponseDTO expectedResponse = new FarmResponseDTO(
                1L,
                "Fazenda Ouro Verde",
                new BigDecimal("150.50"),
                "Sudeste",
                50000,
                "Gleba 4 - Setor Sul",
                10L,
                20L
        );

        when(farmService.getFarmById(1L, principal)).thenReturn(expectedResponse);

        ResponseEntity<FarmResponseDTO> response = farmController.getFarmById(1L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("Fazenda Ouro Verde", response.getBody().name());
        verify(farmService, times(1)).getFarmById(1L, principal);
    }

    /**
     * Testa atualização parcial de fazenda no controller esperando status 200 OK.
     */
    @Test
    @DisplayName("Deve atualizar fazenda parcialmente e retornar status 200 OK")
    void testUpdateFarm() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "adm@ouros.com",
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
        FarmUpdateDTO request = new FarmUpdateDTO(
                "Fazenda Ouro Verde Atualizada",
                new BigDecimal("200.00"),
                null,
                60000,
                null
        );

        FarmResponseDTO expectedResponse = new FarmResponseDTO(
                1L,
                "Fazenda Ouro Verde Atualizada",
                new BigDecimal("200.00"),
                "Sudeste",
                60000,
                "Gleba 4 - Setor Sul",
                10L,
                20L
        );

        when(farmService.updateFarm(1L, request, principal)).thenReturn(expectedResponse);

        ResponseEntity<FarmResponseDTO> response = farmController.updateFarm(1L, request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Fazenda Ouro Verde Atualizada", response.getBody().name());
        assertEquals(60000, response.getBody().poultryCapacity());
        verify(farmService, times(1)).updateFarm(1L, request, principal);
    }

    /**
     * Testa remoção de fazenda no controller esperando status 204 No Content.
     */
    @Test
    @DisplayName("Deve remover fazenda e retornar status 204 No Content")
    void testDeleteFarm() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "adm@ouros.com",
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
        doNothing().when(farmService).deleteFarm(1L, principal);

        ResponseEntity<Void> response = farmController.deleteFarm(1L, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(farmService, times(1)).deleteFarm(1L, principal);
    }
}
