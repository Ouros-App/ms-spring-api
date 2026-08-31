package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.AddressRequestDTO;
import com.ourosapp.springapi.dto.AddressResponseDTO;
import com.ourosapp.springapi.entity.Address;
import com.ourosapp.springapi.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    private Address sampleAddress;
    private AddressRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        sampleAddress = Address.builder()
                .id(1L)
                .zipCode("01310-100")
                .state("SP")
                .city("São Paulo")
                .number("1000")
                .country("BR")
                .build();

        sampleRequest = new AddressRequestDTO(
                "01310-100",
                "SP",
                "São Paulo",
                "1000",
                "BR"
        );
    }

    @Test
    @DisplayName("Deve cadastrar um novo endereço com sucesso")
    void testCreateAddressSuccess() {
        when(addressRepository.save(any(Address.class))).thenReturn(sampleAddress);

        AddressResponseDTO response = addressService.createAddress(sampleRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("01310-100", response.zipCode());
        assertEquals("SP", response.state());
        assertEquals("São Paulo", response.city());
        assertEquals("1000", response.number());
        assertEquals("BR", response.country());

        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    @DisplayName("Deve retornar um endereço existente ao buscar por ID")
    void testGetAddressByIdSuccess() {
        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));

        AddressResponseDTO response = addressService.getAddressById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("01310-100", response.zipCode());
        assertEquals("SP", response.state());
        assertEquals("São Paulo", response.city());
        assertEquals("1000", response.number());
        assertEquals("BR", response.country());

        verify(addressRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao buscar por ID inexistente")
    void testGetAddressByIdNotFound() {
        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> addressService.getAddressById(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("99"));
        verify(addressRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve atualizar um endereço existente com sucesso")
    void testUpdateAddressSuccess() {
        AddressRequestDTO updateRequest = new AddressRequestDTO(
                "13010-001",
                "SP",
                "Campinas",
                "555",
                "BR"
        );

        Address updatedAddress = Address.builder()
                .id(1L)
                .zipCode("13010-001")
                .state("SP")
                .city("Campinas")
                .number("555")
                .country("BR")
                .build();

        when(addressRepository.findById(1L)).thenReturn(Optional.of(sampleAddress));
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        AddressResponseDTO response = addressService.updateAddress(1L, updateRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("13010-001", response.zipCode());
        assertEquals("SP", response.state());
        assertEquals("Campinas", response.city());
        assertEquals("555", response.number());
        assertEquals("BR", response.country());

        verify(addressRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(sampleAddress);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar atualizar endereço inexistente")
    void testUpdateAddressNotFound() {
        AddressRequestDTO updateRequest = new AddressRequestDTO(
                "13010-001",
                "SP",
                "Campinas",
                "555",
                "BR"
        );

        when(addressRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> addressService.updateAddress(99L, updateRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("99"));
        verify(addressRepository, times(1)).findById(99L);
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar cadastrar endereço com payload nulo")
    void testCreateAddressNullPayloadThrowsException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> addressService.createAddress(null)
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar atualizar endereço com payload nulo")
    void testUpdateAddressNullPayloadThrowsException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> addressService.updateAddress(1L, null)
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
        verify(addressRepository, never()).findById(any());
        verify(addressRepository, never()).save(any());
    }
}
