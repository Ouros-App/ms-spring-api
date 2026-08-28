package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.AddressRequestDTO;
import com.ourosapp.springapi.dto.AddressResponseDTO;
import com.ourosapp.springapi.service.AddressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    @Test
    @DisplayName("Deve cadastrar endereço e retornar status 201 Created")
    void testCreateAddress() {
        AddressRequestDTO request = new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR");
        AddressResponseDTO expectedResponse = new AddressResponseDTO(1L, "01310-100", "SP", "São Paulo", "1000", "BR");

        when(addressService.createAddress(request)).thenReturn(expectedResponse);

        ResponseEntity<AddressResponseDTO> response = addressController.createAddress(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("01310-100", response.getBody().zipCode());
        verify(addressService, times(1)).createAddress(request);
    }

    @Test
    @DisplayName("Deve retornar endereço por ID com status 200 OK")
    void testGetAddressById() {
        AddressResponseDTO expectedResponse = new AddressResponseDTO(1L, "01310-100", "SP", "São Paulo", "1000", "BR");

        when(addressService.getAddressById(1L)).thenReturn(expectedResponse);

        ResponseEntity<AddressResponseDTO> response = addressController.getAddressById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(addressService, times(1)).getAddressById(1L);
    }

    @Test
    @DisplayName("Deve atualizar endereço por ID e retornar status 200 OK")
    void testUpdateAddress() {
        AddressRequestDTO request = new AddressRequestDTO("13010-001", "SP", "Campinas", "555", "BR");
        AddressResponseDTO expectedResponse = new AddressResponseDTO(1L, "13010-001", "SP", "Campinas", "555", "BR");

        when(addressService.updateAddress(1L, request)).thenReturn(expectedResponse);

        ResponseEntity<AddressResponseDTO> response = addressController.updateAddress(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("13010-001", response.getBody().zipCode());
        verify(addressService, times(1)).updateAddress(1L, request);
    }
}
