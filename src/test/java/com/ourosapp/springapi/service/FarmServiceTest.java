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
import org.springframework.dao.DataIntegrityViolationException;
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
    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;

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

        adminPrincipal = new UserPrincipal(
                1L, "adm@ouros.com", "pass", "ADM", List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                100L, "emp@empresa.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        farmOwnerPrincipal = new UserPrincipal(
                200L, "produtor@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }

    /**
     * Testa o cadastro de fazenda com ID de endereço existente com perfil ADM.
     */
    @Test
    @DisplayName("Deve cadastrar fazenda com sucesso como ADM usando id_address existente")
    void testCreateFarmWithIdAddressAsAdmSuccess() {
        when(enterpriseRepository.existsById(20L)).thenReturn(true);
        when(addressRepository.existsById(10L)).thenReturn(true);
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        FarmResponseDTO response = farmService.createFarm(sampleRequestWithIdAddress, adminPrincipal);

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

    /**
     * Testa o cadastro de fazenda com perfil de funcionário vinculado à mesma empresa integradora.
     */
    @Test
    @DisplayName("Deve cadastrar fazenda com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void testCreateFarmAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(100L).idEnterprise(20L).build();
        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(enterpriseRepository.existsById(20L)).thenReturn(true);
        when(addressRepository.existsById(10L)).thenReturn(true);
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        FarmResponseDTO response = farmService.createFarm(sampleRequestWithIdAddress, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(companyEmployeeRepository).findById(100L);
        verify(farmRepository).save(any(Farm.class));
    }

    /**
     * Testa restrição de segurança quando funcionário tenta cadastrar fazenda para outra empresa.
     */
    @Test
    @DisplayName("Deve lançar 403 quando COMPANY_EMPLOYEE tentar cadastrar fazenda para outra empresa")
    void testCreateFarmAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(100L).idEnterprise(999L).build();
        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.createFarm(sampleRequestWithIdAddress, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(farmRepository, never()).save(any());
    }

    /**
     * Testa restrição de segurança impedindo produtor rural de cadastrar fazendas.
     */
    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar cadastrar fazenda")
    void testCreateFarmAsFarmOwnerForbidden() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.createFarm(sampleRequestWithIdAddress, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(farmRepository, never()).save(any());
    }

    /**
     * Testa rejeição quando usuário não está autenticado ao cadastrar fazenda.
     */
    @Test
    @DisplayName("Deve lançar 401 ao cadastrar fazenda sem usuário autenticado")
    void testCreateFarmNullPrincipalUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.createFarm(sampleRequestWithIdAddress, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    /**
     * Testa o cadastro de fazenda com criação composta de novo endereço.
     */
    @Test
    @DisplayName("Deve cadastrar fazenda com sucesso criando novo endereço embutido (nested address)")
    void testCreateFarmWithNestedAddressSuccess() {
        when(enterpriseRepository.existsById(20L)).thenReturn(true);
        AddressResponseDTO createdAddress = new AddressResponseDTO(10L, "12345678", "SP", "Campinas", "100", "BR");
        when(addressService.createAddress(sampleAddressRequest)).thenReturn(createdAddress);
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        FarmResponseDTO response = farmService.createFarm(sampleRequestWithNestedAddress, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.idAddress());
        verify(addressService).createAddress(sampleAddressRequest);
        verify(farmRepository).save(any(Farm.class));
    }

    /**
     * Testa lançamento de 404 quando a empresa integradora não existir.
     */
    @Test
    @DisplayName("Deve lançar 404 quando empresa integradora vinculada não existir")
    void testCreateFarmEnterpriseNotFound() {
        when(enterpriseRepository.existsById(20L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.createFarm(sampleRequestWithIdAddress, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Empresa integradora não encontrada"));
        verify(farmRepository, never()).save(any());
    }

    /**
     * Testa lançamento de 404 quando o endereço pré-existente não existir no banco.
     */
    @Test
    @DisplayName("Deve lançar 404 quando endereço vinculado via id_address não existir")
    void testCreateFarmAddressNotFound() {
        when(enterpriseRepository.existsById(20L)).thenReturn(true);
        when(addressRepository.existsById(10L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.createFarm(sampleRequestWithIdAddress, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Endereço não encontrado"));
        verify(farmRepository, never()).save(any());
    }

    /**
     * Testa lançamento de 400 quando nenhuma informação de endereço for enviada.
     */
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
                farmService.createFarm(requestWithoutAddress, adminPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(farmRepository, never()).save(any());
    }

    /**
     * Testa listagem de fazendas para funcionário da empresa integradora filtrando pelo id da empresa.
     */
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

    /**
     * Testa lançamento de 404 quando o funcionário autenticado não for localizado no banco.
     */
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

    /**
     * Testa listagem de fazenda para produtor rural retornando sua fazenda vinculada.
     */
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

    /**
     * Testa retorno de lista vazia para produtor quando sua fazenda vinculada não for encontrada.
     */
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

    /**
     * Testa listagem de fazendas para produtor rural com idFarm nulo.
     */
    @Test
    @DisplayName("Deve retornar lista vazia quando produtor rural não possuir fazenda vinculada (idFarm nulo)")
    void testGetFarmsForFarmOwnerNullIdFarm() {
        UserPrincipal principal = new UserPrincipal(
                200L, "produtor@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
        FarmOwner owner = FarmOwner.builder()
                .id(200L)
                .idFarm(null)
                .build();

        when(farmOwnerRepository.findById(200L)).thenReturn(Optional.of(owner));

        List<FarmResponseDTO> farms = farmService.getFarmsForUser(principal);

        assertNotNull(farms);
        assertTrue(farms.isEmpty());
        verify(farmRepository, never()).findById(any());
    }

    /**
     * Testa lançamento de 404 quando produtor rural não for localizado no banco.
     */
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

    /**
     * Testa listagem geral de fazendas quando usuário autenticado for ADM.
     */
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

    /**
     * Testa lançamento de 401 ao listar fazendas com usuário nulo.
     */
    @Test
    @DisplayName("Deve lançar 401 quando principal for nulo ao listar fazendas")
    void testGetFarmsForUserNullPrincipal() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmsForUser(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    /**
     * Testa lançamento de 403 ao listar fazendas com perfil não reconhecido.
     */
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

    /**
     * Testa busca de fazenda por ID como ADM.
     */
    @Test
    @DisplayName("Deve buscar fazenda por ID com sucesso como ADM")
    void testGetFarmByIdAsAdmSuccess() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        FarmResponseDTO response = farmService.getFarmById(1L, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Fazenda Ouro Verde", response.name());
        verify(farmRepository).findById(1L);
    }

    /**
     * Testa busca de fazenda por ID para funcionário vinculado à mesma empresa da fazenda.
     */
    @Test
    @DisplayName("Deve buscar fazenda por ID com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void testGetFarmByIdAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(100L).idEnterprise(20L).build();
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.of(employee));

        FarmResponseDTO response = farmService.getFarmById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    /**
     * Testa restrição de segurança impedindo funcionário de buscar fazenda de outra empresa integradora.
     */
    @Test
    @DisplayName("Deve lançar 403 ao buscar fazenda por ID como COMPANY_EMPLOYEE de outra empresa")
    void testGetFarmByIdAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(100L).idEnterprise(999L).build();
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmById(1L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Testa busca de fazenda por ID para produtor proprietário da respectiva fazenda.
     */
    @Test
    @DisplayName("Deve buscar fazenda por ID com sucesso como FARM_OWNER da própria fazenda")
    void testGetFarmByIdAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(200L).idFarm(1L).build();
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmOwnerRepository.findById(200L)).thenReturn(Optional.of(owner));

        FarmResponseDTO response = farmService.getFarmById(1L, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    /**
     * Testa restrição de segurança impedindo produtor de visualizar fazendas de terceiros.
     */
    @Test
    @DisplayName("Deve lançar 403 ao buscar fazenda por ID como FARM_OWNER de outra fazenda")
    void testGetFarmByIdAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(200L).idFarm(999L).build();
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmOwnerRepository.findById(200L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmById(1L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Testa lançamento de 404 ao buscar fazenda inexistente por ID.
     */
    @Test
    @DisplayName("Deve lançar 404 ao buscar fazenda por ID inexistente")
    void testGetFarmByIdNotFound() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.getFarmById(99L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Testa atualização parcial de fazenda com usuário ADM.
     */
    @Test
    @DisplayName("Deve atualizar parcialmente a fazenda com sucesso como ADM")
    void testUpdateFarmAsAdmSuccess() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FarmUpdateDTO updateDTO = new FarmUpdateDTO(
                "Fazenda Ouro Verde Atualizada",
                new BigDecimal("250.00"),
                "Centro-Oeste",
                70000,
                "Gleba 5"
        );

        FarmResponseDTO response = farmService.updateFarm(1L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals("Fazenda Ouro Verde Atualizada", response.name());
        assertEquals(new BigDecimal("250.00"), response.areaProperty());
        assertEquals("Centro-Oeste", response.region());
        assertEquals(70000, response.poultryCapacity());
        assertEquals("Gleba 5", response.place());
        verify(farmRepository).save(any(Farm.class));
    }

    /**
     * Testa atualização parcial de fazenda com funcionário da mesma empresa.
     */
    @Test
    @DisplayName("Deve atualizar parcialmente a fazenda com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void testUpdateFarmAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(100L).idEnterprise(20L).build();
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.of(employee));
        when(farmRepository.save(any(Farm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FarmUpdateDTO updateDTO = new FarmUpdateDTO("Novo Nome", null, null, null, null);

        FarmResponseDTO response = farmService.updateFarm(1L, updateDTO, employeePrincipal);

        assertNotNull(response);
        assertEquals("Novo Nome", response.name());
        verify(farmRepository).save(any(Farm.class));
    }

    /**
     * Testa restrição de segurança impedindo funcionário de atualizar fazenda de outra empresa integradora.
     */
    @Test
    @DisplayName("Deve lançar 403 ao atualizar fazenda como COMPANY_EMPLOYEE de outra empresa")
    void testUpdateFarmAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(100L).idEnterprise(999L).build();
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.of(employee));

        FarmUpdateDTO updateDTO = new FarmUpdateDTO("Novo Nome", null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.updateFarm(1L, updateDTO, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(farmRepository, never()).save(any(Farm.class));
    }

    /**
     * Testa atualização de fazenda sem modificações quando DTO for vazio.
     */
    @Test
    @DisplayName("Deve retornar fazenda inalterada quando DTO de atualização não possuir campos (hasUpdates = false)")
    void testUpdateFarmNoUpdates() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        FarmUpdateDTO emptyUpdate = new FarmUpdateDTO(null, null, null, null, null);

        FarmResponseDTO response = farmService.updateFarm(1L, emptyUpdate, adminPrincipal);

        assertNotNull(response);
        assertEquals("Fazenda Ouro Verde", response.name());
        verify(farmRepository, never()).save(any(Farm.class));
    }

    /**
     * Testa lançamento de 404 ao atualizar fazenda inexistente.
     */
    @Test
    @DisplayName("Deve lançar 404 ao atualizar fazenda inexistente")
    void testUpdateFarmNotFound() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        FarmUpdateDTO updateDTO = new FarmUpdateDTO("Novo Nome", null, null, null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.updateFarm(99L, updateDTO, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(farmRepository, never()).save(any(Farm.class));
    }

    /**
     * Testa remoção de fazenda com perfil ADM.
     */
    @Test
    @DisplayName("Deve remover fazenda com sucesso como ADM")
    void testDeleteFarmAsAdmSuccess() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        doNothing().when(farmRepository).delete(sampleFarm);

        assertDoesNotThrow(() -> farmService.deleteFarm(1L, adminPrincipal));

        verify(farmRepository).findById(1L);
        verify(farmRepository).delete(sampleFarm);
    }

    /**
     * Testa lançamento de 409 Conflict ao remover fazenda com vínculos/restrições no banco de dados.
     */
    @Test
    @DisplayName("Deve lançar 409 Conflict quando remoção violar integridade referencial")
    void testDeleteFarmDataIntegrityViolationConflict() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        doThrow(new DataIntegrityViolationException("Violação de FK")).when(farmRepository).delete(sampleFarm);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.deleteFarm(1L, adminPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Não é possível remover a fazenda pois existem registros vinculados a ela", ex.getReason());
        verify(farmRepository).delete(sampleFarm);
    }

    /**
     * Testa remoção de fazenda com funcionário da mesma empresa integradora.
     */
    @Test
    @DisplayName("Deve remover fazenda com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void testDeleteFarmAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(100L).idEnterprise(20L).build();
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(100L)).thenReturn(Optional.of(employee));
        doNothing().when(farmRepository).delete(sampleFarm);

        assertDoesNotThrow(() -> farmService.deleteFarm(1L, employeePrincipal));

        verify(farmRepository).findById(1L);
        verify(farmRepository).delete(sampleFarm);
    }

    /**
     * Testa restrição de segurança impedindo produtor de remover fazenda.
     */
    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar remover fazenda")
    void testDeleteFarmAsFarmOwnerForbidden() {
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.deleteFarm(1L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(farmRepository, never()).delete(any());
    }

    /**
     * Testa lançamento de 404 ao tentar remover fazenda inexistente.
     */
    @Test
    @DisplayName("Deve lançar 404 ao remover fazenda inexistente")
    void testDeleteFarmNotFound() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                farmService.deleteFarm(99L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(farmRepository, never()).delete(any());
    }
}
