package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.lot.LotRequestDTO;
import com.ourosapp.springapi.dto.lot.LotResponseDTO;
import com.ourosapp.springapi.dto.lot.LotUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.LotService;
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
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o controlador REST {@link LotController}.
 */
@ExtendWith(MockitoExtension.class)
class LotControllerTest {

    @Mock
    private LotService lotService;

    @InjectMocks
    private LotController lotController;

    private final UserPrincipal principal = new UserPrincipal(
            1L,
            "adm@ouros.com",
            null,
            "ADM",
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    @Test
    @DisplayName("Deve cadastrar lote e retornar status 201 Created com cabeçalho Location")
    void testCreateLot() {
        MockHttpServletRequest requestContext = new MockHttpServletRequest();
        requestContext.setServerName("localhost");
        requestContext.setRequestURI("/lots");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestContext));

        try {
            LotRequestDTO request = new LotRequestDTO(
                    50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                    new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
            );
            LotResponseDTO expectedResponse = new LotResponseDTO(
                    10L, 50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                    new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
            );

            when(lotService.createLot(request, principal)).thenReturn(expectedResponse);

            ResponseEntity<LotResponseDTO> response = lotController.createLot(request, principal);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(10L, response.getBody().id());
            assertNotNull(response.getHeaders().getLocation());
            assertTrue(response.getHeaders().getLocation().getPath().endsWith("/10"));
            verify(lotService, times(1)).createLot(request, principal);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    @DisplayName("Deve listar lotes para o usuário com status 200 OK")
    void testGetLotsForUser() {
        LotResponseDTO lot1 = new LotResponseDTO(
                1L, 50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
        );
        when(lotService.getLotsForUser(2L, 1L, principal)).thenReturn(List.of(lot1));

        ResponseEntity<List<LotResponseDTO>> response = lotController.getLotsForUser(2L, 1L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).id());
        verify(lotService, times(1)).getLotsForUser(2L, 1L, principal);
    }

    @Test
    @DisplayName("Deve buscar lote por ID com status 200 OK")
    void testGetLotById() {
        LotResponseDTO expectedResponse = new LotResponseDTO(
                1L, 50000, 48500, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 1500, 12500.50, 1L, 2L
        );
        when(lotService.getLotById(1L, principal)).thenReturn(expectedResponse);

        ResponseEntity<LotResponseDTO> response = lotController.getLotById(1L, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(lotService, times(1)).getLotById(1L, principal);
    }

    @Test
    @DisplayName("Deve atualizar lote parcialmente e retornar status 200 OK")
    void testUpdateLot() {
        LotUpdateDTO request = new LotUpdateDTO(
                null, 49000, null, LocalDate.of(2026, 10, 15),
                new BigDecimal("2.9500"), 1000, 13000.0
        );
        LotResponseDTO expectedResponse = new LotResponseDTO(
                1L, 50000, 49000, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.9500"), 1000, 13000.0, 1L, 2L
        );
        when(lotService.updateLot(1L, request, principal)).thenReturn(expectedResponse);

        ResponseEntity<LotResponseDTO> response = lotController.updateLot(1L, request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(49000, response.getBody().deliveredChickens());
        verify(lotService, times(1)).updateLot(1L, request, principal);
    }

    @Test
    @DisplayName("Deve remover lote e retornar status 204 No Content")
    void testDeleteLot() {
        doNothing().when(lotService).deleteLot(1L, principal);

        ResponseEntity<Void> response = lotController.deleteLot(1L, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(lotService, times(1)).deleteLot(1L, principal);
    }
}
