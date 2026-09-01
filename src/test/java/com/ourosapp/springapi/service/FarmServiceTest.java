package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.AddressRequestDTO;
import com.ourosapp.springapi.dto.AddressResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AddressRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para a camada de serviço {@link FarmService}.
 */
@ExtendWith(MockitoExtension.class)
class FarmServiceTest {

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private FarmService farmService;

    private Farm sampleFarm;
    private FarmRequestDTO sampleRequestWithIdAddress;
    private FarmRequestDTO sampleRequestWithNestedAddress;
    private AddressRequestDTO sampleAddressRequest;

    @BeforeEach
    void setUp() {
        sampleFarm = Farm.builder()
                .id(1L)
                .name("Fazenda Ouro Verde")
                .areaProperty(new BigDecimal("150.50"))
                .region("Sudeste")
                .poultryCapacity(50000)
                .place("Gleba 4 - Setor Sul")
                .idAddress(10L)
                .idEnterprise(20L)
                .build();

        sampleRequestWithIdAddress = new FarmRequestDTO(
                "Fazenda Ouro Verde",
                new BigDecimal("150.50"),
                "Sudeste",
                50000,
                "Gleba 4 - Setor Sul",
                10L,
                null,
                20L
        );

        sampleAddressRequest = new AddressRequestDTO(
                "12345678",
                "SP",
                "Campinas",
                "100",
                "BR"
        );

        sampleRequestWithNestedAddress = new FarmRequestDTO(
                "Fazenda Ouro Verde",
                new BigDecimal("150.50"),
                "Sudeste",
                50000,
                "Gleba 4 - Setor Sul",
                null,
                sampleAddressRequest,
                20L
        );
    }

    @Test
    @DisplayName("Deve cadastrar fazenda com sucesso usando id_address existente")
    void testCreateFarmWithIdAddressSuccess() {
        when(enterpriseRepository.existsById(20L)).thenReturn(true);
        when(addressRepository.existsById(10L)).thenReturn(true);
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        FarmResponseDTO response = farmService.createFarm(sampleRequestWithIdAddress);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Fazenda Ouro Verde", response.name());
        assertEquals(new BigDecimal("150.50"), response.areaProperty());
        assertEquals("Sudeste", response.region());
        assertEquals(50000, response.poultryCapacity());
        assertEquals("Gleba 4 - Setor Sul", response.place());
        assertEquals(10L, response.idAddress());
        assertEquals(20L, response.idEnterprise());

        verify(enterpriseRepository).existsById(20L);
        verify(addressRepository).existsById(10L);
        verify(farmRepository).save(any(Farm.class));
    }

    @Test
    @DisplayName("Deve cadastrar fazenda com sucesso criando novo endereço embutido (nested address)")
    void testCreateFarmWithNestedAddressSuccess() {
        when(enterpriseRepository.existsById(20L)).thenReturn(true);
        AddressResponseDTO createdAddress = new AddressResponseDTO(10L, "12345678", "SP", "Campinas", "100", "BR");
        when(addressService.createAddress(sampleAddressRequest)).thenReturn(createdAddress);
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        FarmResponseDTO response = farmService.createFarm(sampleRequestWithNestedAddress);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.idAddress());
        verify(addressService).createAddress(sampleAddressRequest);
        verify(farmRepository).save(any(Farm.class));
    }

    @Test
    @DisplayName("Deve lançar 404 quando empresa integradora vinculada não existir")
    void testCreateFarmEnterpriseNotFound() {
        when(enterpriseRepository.existsById(20L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.createFarm(sampleRequestWithIdAddress)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Empresa integradora não encontrada"));
        verify(farmRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 404 quando endereço vinculado via id_address não existir")
    void testCreateFarmAddressNotFound() {
        when(enterpriseRepository.existsById(20L)).thenReturn(true);
        when(addressRepository.existsById(10L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.createFarm(sampleRequestWithIdAddress)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Endereço não encontrado"));
        verify(farmRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando nem id_address nem address forem informados")
    void testCreateFarmNoAddressInfo() {
        when(enterpriseRepository.existsById(20L)).thenReturn(true);
        FarmRequestDTO requestWithoutAddress = new FarmRequestDTO(
                "Fazenda",
                new BigDecimal("100"),
                "Sul",
                1000,
                "Local",
                null,
                null,
                20L
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.createFarm(requestWithoutAddress)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(farmRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar fazendas para COMPANY_EMPLOYEE filtrando pelo id_enterprise")
    void testGetFarmsForCompanyEmployee() {
        UserPrincipal principal = new UserPrincipal(
                100L, "emp@empresa.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );
        CompanyEmployee employee = CompanyEmployee.builder()
                .id(100L)
                .idEnterprise(20L)
                .build();

        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(20L)).thenReturn(List.of(sampleFarm));

        List<FarmResponseDTO> farms = farmService.getFarmsForUser(principal);

        assertNotNull(farms);
        assertEquals(1, farms.size());
        assertEquals("Fazenda Ouro Verde", farms.get(0).name());
        verify(companyEmployeeRepository).findById(100L);
        verify(farmRepository).findAllByIdEnterprise(20L);
    }

    @Test
    @DisplayName("Deve lançar 404 quando funcionário autenticado não for encontrado no banco")
    void testGetFarmsForCompanyEmployeeNotFound() {
        UserPrincipal principal = new UserPrincipal(
                100L, "emp@empresa.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );
        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmsForUser(principal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar fazenda para FARM_OWNER buscando pelo id_farm vinculado")
    void testGetFarmsForFarmOwner() {
        UserPrincipal principal = new UserPrincipal(
                200L, "produtor@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
        FarmOwner owner = FarmOwner.builder()
                .id(200L)
                .idFarm(1L)
                .build();

        when(farmOwnerRepository.findById(200L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        List<FarmResponseDTO> farms = farmService.getFarmsForUser(principal);

        assertNotNull(farms);
        assertEquals(1, farms.size());
        assertEquals(1L, farms.get(0).id());
        verify(farmOwnerRepository).findById(200L);
        verify(farmRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve retornar lista vazia para FARM_OWNER quando fazenda vinculada não for encontrada")
    void testGetFarmsForFarmOwnerFarmNotFound() {
        UserPrincipal principal = new UserPrincipal(
                200L, "produtor@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
        FarmOwner owner = FarmOwner.builder()
                .id(200L)
                .idFarm(999L)
                .build();

        when(farmOwnerRepository.findById(200L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        List<FarmResponseDTO> farms = farmService.getFarmsForUser(principal);

        assertNotNull(farms);
        assertTrue(farms.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar 404 quando produtor autenticado não for encontrado no banco")
    void testGetFarmsForFarmOwnerNotFound() {
        UserPrincipal principal = new UserPrincipal(
                200L, "produtor@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
        when(farmOwnerRepository.findById(200L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmsForUser(principal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar todas as fazendas quando usuário for ADM")
    void testGetFarmsForAdm() {
        UserPrincipal principal = new UserPrincipal(
                1L, "adm@ouros.com", "pass", "ADM", List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
        when(farmRepository.findAll()).thenReturn(List.of(sampleFarm));

        List<FarmResponseDTO> farms = farmService.getFarmsForUser(principal);

        assertNotNull(farms);
        assertEquals(1, farms.size());
        verify(farmRepository).findAll();
    }

    @Test
    @DisplayName("Deve lançar 401 quando principal for nulo ao listar fazendas")
    void testGetFarmsForUserNullPrincipal() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmsForUser(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 quando role do usuário não for reconhecida")
    void testGetFarmsForUserForbiddenRole() {
        UserPrincipal principal = new UserPrincipal(
                1L, "guest@ouros.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmsForUser(principal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve buscar fazenda por ID com sucesso")
    void testGetFarmByIdSuccess() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        FarmResponseDTO response = farmService.getFarmById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Fazenda Ouro Verde", response.name());
        verify(farmRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar fazenda por ID inexistente")
    void testGetFarmByIdNotFound() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmById(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve atualizar parcialmente a fazenda com sucesso")
    void testUpdateFarmSuccess() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FarmUpdateDTO updateDTO = new FarmUpdateDTO(
                "Fazenda Ouro Verde Atualizada",
                new BigDecimal("250.00"),
                "Centro-Oeste",
                70000,
                "Gleba 5"
        );

        FarmResponseDTO response = farmService.updateFarm(1L, updateDTO);

        assertNotNull(response);
        assertEquals("Fazenda Ouro Verde Atualizada", response.name());
        assertEquals(new BigDecimal("250.00"), response.areaProperty());
        assertEquals("Centro-Oeste", response.region());
        assertEquals(70000, response.poultryCapacity());
        assertEquals("Gleba 5", response.place());
        verify(farmRepository).save(any(Farm.class));
    }

    @Test
    @DisplayName("Deve retornar fazenda inalterada quando DTO de atualização não possuir campos (hasUpdates = false)")
    void testUpdateFarmNoUpdates() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        FarmUpdateDTO emptyUpdate = new FarmUpdateDTO(null, null, null, null, null);

        FarmResponseDTO response = farmService.updateFarm(1L, emptyUpdate);

        assertNotNull(response);
        assertEquals("Fazenda Ouro Verde", response.name());
        verify(farmRepository, never()).save(any(Farm.class));
    }

    @Test
    @DisplayName("Deve lançar 404 ao atualizar fazenda inexistente")
    void testUpdateFarmNotFound() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        FarmUpdateDTO updateDTO = new FarmUpdateDTO("Novo Nome", null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.updateFarm(99L, updateDTO)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(farmRepository, never()).save(any(Farm.class));
    }

    @Test
    @DisplayName("Deve remover fazenda com sucesso")
    void testDeleteFarmSuccess() {
        when(farmRepository.existsById(1L)).thenReturn(true);
        doNothing().when(farmRepository).deleteById(1L);

        assertDoesNotThrow(() -> farmService.deleteFarm(1L));

        verify(farmRepository).existsById(1L);
        verify(farmRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Deve lançar 404 ao remover fazenda inexistente")
    void testDeleteFarmNotFound() {
        when(farmRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.deleteFarm(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(farmRepository, never()).deleteById(any());
    }
}
