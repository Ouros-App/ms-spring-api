package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.EnterpriseRequestDTO;
import com.ourosapp.springapi.dto.EnterpriseResponseDTO;
import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.repository.AddressRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnterpriseServiceTest {

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private EnterpriseService enterpriseService;

    private Enterprise sampleEnterprise;
    private EnterpriseRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        sampleEnterprise = Enterprise.builder()
                .id(1L)
                .name("Agro Ouros S.A.")
                .email("contato@agroouros.com.br")
                .documentNumber("12345678000195")
                .telephone("11999999999")
                .idAddress(10L)
                .build();

        sampleRequest = new EnterpriseRequestDTO(
                "Agro Ouros S.A.",
                "contato@agroouros.com.br",
                "12345678000195",
                "11999999999",
                10L
        );
    }

    @Test
    @DisplayName("Deve cadastrar uma nova empresa com sucesso quando o endereço existir")
    void testCreateEnterpriseSuccess() {
        when(addressRepository.existsById(10L)).thenReturn(true);
        when(enterpriseRepository.save(any(Enterprise.class))).thenReturn(sampleEnterprise);

        EnterpriseResponseDTO response = enterpriseService.createEnterprise(sampleRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Agro Ouros S.A.", response.name());
        assertEquals("contato@agroouros.com.br", response.email());
        assertEquals("12345678000195", response.documentNumber());
        assertEquals("11999999999", response.telephone());
        assertEquals(10L, response.idAddress());

        verify(addressRepository, times(1)).existsById(10L);
        verify(enterpriseRepository, times(1)).save(any(Enterprise.class));
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar cadastrar empresa com endereço inexistente")
    void testCreateEnterpriseAddressNotFound() {
        when(addressRepository.existsById(10L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> enterpriseService.createEnterprise(sampleRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("10"));
        verify(addressRepository, times(1)).existsById(10L);
        verify(enterpriseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar cadastrar empresa com payload nulo")
    void testCreateEnterpriseNullPayloadThrowsException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> enterpriseService.createEnterprise(null)
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
        verify(enterpriseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar uma empresa existente ao buscar por ID")
    void testGetEnterpriseByIdSuccess() {
        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(sampleEnterprise));

        EnterpriseResponseDTO response = enterpriseService.getEnterpriseById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Agro Ouros S.A.", response.name());
        assertEquals("contato@agroouros.com.br", response.email());
        assertEquals("12345678000195", response.documentNumber());
        assertEquals("11999999999", response.telephone());
        assertEquals(10L, response.idAddress());

        verify(enterpriseRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao buscar empresa por ID inexistente")
    void testGetEnterpriseByIdNotFound() {
        when(enterpriseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> enterpriseService.getEnterpriseById(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("99"));
        verify(enterpriseRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve retornar lista com todas as empresas cadastradas")
    void testGetAllEnterprisesSuccess() {
        Enterprise enterprise2 = Enterprise.builder()
                .id(2L)
                .name("Ouros Sul")
                .email("sul@agroouros.com.br")
                .documentNumber("98765432000100")
                .telephone("41988887777")
                .idAddress(20L)
                .build();

        when(enterpriseRepository.findAll()).thenReturn(List.of(sampleEnterprise, enterprise2));

        List<EnterpriseResponseDTO> result = enterpriseService.getAllEnterprises();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Agro Ouros S.A.", result.get(0).name());
        assertEquals("Ouros Sul", result.get(1).name());

        verify(enterpriseRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não existirem empresas cadastradas")
    void testGetAllEnterprisesEmpty() {
        when(enterpriseRepository.findAll()).thenReturn(List.of());

        List<EnterpriseResponseDTO> result = enterpriseService.getAllEnterprises();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(enterpriseRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve atualizar uma empresa existente com sucesso quando o endereço existir")
    void testUpdateEnterpriseSuccess() {
        EnterpriseRequestDTO updateRequest = new EnterpriseRequestDTO(
                "Agro Ouros Renovada S.A.",
                "novo-contato@agroouros.com.br",
                "12345678000195",
                "11988887777",
                15L
        );

        Enterprise updatedEnterprise = Enterprise.builder()
                .id(1L)
                .name("Agro Ouros Renovada S.A.")
                .email("novo-contato@agroouros.com.br")
                .documentNumber("12345678000195")
                .telephone("11988887777")
                .idAddress(15L)
                .build();

        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(sampleEnterprise));
        when(addressRepository.existsById(15L)).thenReturn(true);
        when(enterpriseRepository.save(any(Enterprise.class))).thenReturn(updatedEnterprise);

        EnterpriseResponseDTO response = enterpriseService.updateEnterprise(1L, updateRequest);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Agro Ouros Renovada S.A.", response.name());
        assertEquals("novo-contato@agroouros.com.br", response.email());
        assertEquals("11988887777", response.telephone());
        assertEquals(15L, response.idAddress());

        verify(enterpriseRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).existsById(15L);
        verify(enterpriseRepository, times(1)).save(sampleEnterprise);
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar atualizar empresa inexistente")
    void testUpdateEnterpriseNotFound() {
        EnterpriseRequestDTO updateRequest = new EnterpriseRequestDTO(
                "Agro Ouros Renovada S.A.",
                "novo-contato@agroouros.com.br",
                "12345678000195",
                "11988887777",
                10L
        );

        when(enterpriseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> enterpriseService.updateEnterprise(99L, updateRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("99"));
        verify(enterpriseRepository, times(1)).findById(99L);
        verify(enterpriseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar atualizar empresa com endereço inexistente")
    void testUpdateEnterpriseAddressNotFound() {
        EnterpriseRequestDTO updateRequest = new EnterpriseRequestDTO(
                "Agro Ouros Renovada S.A.",
                "novo-contato@agroouros.com.br",
                "12345678000195",
                "11988887777",
                999L
        );

        when(enterpriseRepository.findById(1L)).thenReturn(Optional.of(sampleEnterprise));
        when(addressRepository.existsById(999L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> enterpriseService.updateEnterprise(1L, updateRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("999"));
        verify(enterpriseRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).existsById(999L);
        verify(enterpriseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar atualizar empresa com payload nulo")
    void testUpdateEnterpriseNullPayloadThrowsException() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> enterpriseService.updateEnterprise(1L, null)
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
        verify(enterpriseRepository, never()).findById(any());
        verify(enterpriseRepository, never()).save(any());
    }
}
