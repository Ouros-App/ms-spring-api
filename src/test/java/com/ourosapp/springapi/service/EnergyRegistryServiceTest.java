package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryRequestDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryResponseDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.EnergyRegistry;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnergyRegistryRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para a camada de serviço {@link EnergyRegistryService}.
 */
@ExtendWith(MockitoExtension.class)
class EnergyRegistryServiceTest {

    @Mock
    private EnergyRegistryRepository energyRegistryRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private EnergyRegistryService energyRegistryService;

    private UserPrincipal admPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal ownerPrincipal;
    private Farm farm;
    private EnergyRegistry energyRegistry;
    private EnergyRegistryRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        admPrincipal = new UserPrincipal(1L, "adm@ouros.com", null, ADM, List.of(new SimpleGrantedAuthority("ROLE_ADM")));
        employeePrincipal = new UserPrincipal(2L, "employee@ouros.com", null, COMPANY_EMPLOYEE, List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE")));
        ownerPrincipal = new UserPrincipal(3L, "owner@ouros.com", null, FARM_OWNER, List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER")));

        farm = Farm.builder()
                .id(10L)
                .name("Fazenda Teste")
                .areaProperty(new BigDecimal("100.00"))
                .region("Sudeste")
                .poultryCapacity(20000)
                .place("Gleba 1")
                .idAddress(100L)
                .idEnterprise(50L)
                .build();

        energyRegistry = EnergyRegistry.builder()
                .id(1L)
                .registrationDate(LocalDate.now())
                .energyConsumption(new BigDecimal("500.00"))
                .idFarm(10L)
                .build();

        requestDTO = new EnergyRegistryRequestDTO(
                LocalDate.now(),
                new BigDecimal("500.00"),
                10L
        );
    }

    /** Verifica a criação de um registro de energia por um administrador. */
    @Test
    @DisplayName("Deve criar registro de energia com sucesso para usuário ADM")
    void deveCriarRegistroDeEnergiaComSucessoParaAdm() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(energyRegistryRepository.save(any(EnergyRegistry.class))).thenReturn(energyRegistry);

        EnergyRegistryResponseDTO response = energyRegistryService.createEnergyRegistry(requestDTO, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(new BigDecimal("500.00"), response.energyConsumption());
        assertEquals(10L, response.idFarm());
        verify(energyRegistryRepository, times(1)).save(any(EnergyRegistry.class));
    }

    /** Verifica a criação de um registro por funcionário da empresa da fazenda. */
    @Test
    @DisplayName("Deve criar registro de energia com sucesso para Funcionário da mesma empresa")
    void deveCriarRegistroDeEnergiaComSucessoParaFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(energyRegistryRepository.save(any(EnergyRegistry.class))).thenReturn(energyRegistry);

        EnergyRegistryResponseDTO response = energyRegistryService.createEnergyRegistry(requestDTO, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(energyRegistryRepository, times(1)).save(any(EnergyRegistry.class));
    }

    /** Verifica a rejeição da criação quando não há usuário autenticado. */
    @Test
    @DisplayName("Deve lançar exceção 401 quando usuário não estiver autenticado ao criar registro")
    void deveLancarExcecaoQuandoUsuarioNaoAutenticadoAoCriar() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.createEnergyRegistry(requestDTO, null)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    /** Verifica a rejeição da criação quando a fazenda não existe. */
    @Test
    @DisplayName("Deve lançar exceção 404 quando a fazenda informada não for encontrada ao criar registro")
    void deveLancarExcecaoQuandoFazendaNaoEncontradaAoCriar() {
        when(farmRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.createEnergyRegistry(requestDTO, admPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Fazenda não encontrada"));
    }

    /** Verifica a rejeição da criação por funcionário de outra empresa. */
    @Test
    @DisplayName("Deve lançar exceção 403 quando funcionário for de outra empresa ao criar registro")
    void deveLancarExcecaoQuandoFuncionarioDeOutraEmpresaAoCriar() {
        CompanyEmployee employeeDeOutraEmpresa = CompanyEmployee.builder().id(2L).idEnterprise(99L).build();
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employeeDeOutraEmpresa));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.createEnergyRegistry(requestDTO, employeePrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /** Verifica o tratamento de conflito de integridade durante a criação. */
    @Test
    @DisplayName("Deve lançar exceção 409 quando ocorrer violação de integridade ao salvar")
    void deveLancarExcecaoQuandoViolacaoDeIntegridadeAoSalvar() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(energyRegistryRepository.save(any(EnergyRegistry.class)))
                .thenThrow(new DataIntegrityViolationException("Erro de integridade"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.createEnergyRegistry(requestDTO, admPrincipal)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    /** Verifica a listagem de todos os registros para um administrador. */
    @Test
    @DisplayName("Deve listar todos os registros de energia para perfil ADM")
    void deveListarTodosOsRegistrosParaAdm() {
        when(energyRegistryRepository.findAll()).thenReturn(List.of(energyRegistry));

        List<EnergyRegistryResponseDTO> list = energyRegistryService.getEnergyRegistriesForUser(null, admPrincipal);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).id());
    }

    /** Verifica a listagem de registros das fazendas da empresa do funcionário. */
    @Test
    @DisplayName("Deve listar registros de energia para Funcionário considerando fazendas da sua empresa")
    void deveListarRegistrosParaFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of(farm));
        when(energyRegistryRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of(energyRegistry));

        List<EnergyRegistryResponseDTO> list = energyRegistryService.getEnergyRegistriesForUser(null, employeePrincipal);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).id());
    }

    /** Verifica a lista vazia para funcionário cuja empresa não possui fazendas. */
    @Test
    @DisplayName("Deve retornar lista vazia para Funcionário sem fazendas em sua empresa")
    void deveRetornarListaVaziaParaFuncionarioSemFazendas() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(50L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of());

        List<EnergyRegistryResponseDTO> list = energyRegistryService.getEnergyRegistriesForUser(null, employeePrincipal);

        assertTrue(list.isEmpty());
    }

    /** Verifica a listagem de registros da fazenda vinculada ao produtor rural. */
    @Test
    @DisplayName("Deve listar registros de energia para Produtor Rural na sua fazenda")
    void deveListarRegistrosParaProdutorRural() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(energyRegistryRepository.findByIdFarm(10L)).thenReturn(List.of(energyRegistry));

        List<EnergyRegistryResponseDTO> list = energyRegistryService.getEnergyRegistriesForUser(null, ownerPrincipal);

        assertEquals(1, list.size());
    }

    /** Verifica a listagem filtrada por uma fazenda específica. */
    @Test
    @DisplayName("Deve filtrar registros por fazenda específica quando farmIdFilter for informado")
    void deveFiltrarRegistrosPorFazendaEspecifica() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(energyRegistryRepository.findByIdFarm(10L)).thenReturn(List.of(energyRegistry));

        List<EnergyRegistryResponseDTO> list = energyRegistryService.getEnergyRegistriesForUser(10L, admPrincipal);

        assertEquals(1, list.size());
    }

    /** Verifica a consulta bem-sucedida de um registro por identificador. */
    @Test
    @DisplayName("Deve buscar registro de energia por ID com sucesso")
    void deveBuscarRegistroPorIdComSucesso() {
        when(energyRegistryRepository.findById(1L)).thenReturn(Optional.of(energyRegistry));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        EnergyRegistryResponseDTO response = energyRegistryService.getEnergyRegistryById(1L, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    /** Verifica a rejeição da consulta quando o registro não existe. */
    @Test
    @DisplayName("Deve lançar exceção 404 ao buscar registro por ID inexistente")
    void deveLancarExcecaoAoBuscarPorIdInexistente() {
        when(energyRegistryRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.getEnergyRegistryById(99L, admPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /** Verifica a inferência da fazenda ao criar um registro como produtor rural. */
    @Test
    @DisplayName("Deve criar registro de energia com sucesso para Produtor Rural sem id_farm no request")
    void deveCriarRegistroDeEnergiaComSucessoParaProdutorRuralSemIdFarm() {
        EnergyRegistryRequestDTO requestSemFarm = new EnergyRegistryRequestDTO(
                LocalDate.now(),
                new BigDecimal("300.00"),
                null
        );
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(energyRegistryRepository.save(any(EnergyRegistry.class))).thenReturn(energyRegistry);

        EnergyRegistryResponseDTO response = energyRegistryService.createEnergyRegistry(requestSemFarm, ownerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(energyRegistryRepository, times(1)).save(any(EnergyRegistry.class));
    }

    /** Verifica a rejeição da criação sem fazenda por um administrador. */
    @Test
    @DisplayName("Deve lançar exceção 400 quando ADM tentar criar registro sem id_farm")
    void deveLancarExcecao400QuandoAdmCriarSemIdFarm() {
        EnergyRegistryRequestDTO requestSemFarm = new EnergyRegistryRequestDTO(
                LocalDate.now(),
                new BigDecimal("300.00"),
                null
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.createEnergyRegistry(requestSemFarm, admPrincipal)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("O ID da fazenda é obrigatório"));
    }

    /** Verifica a rejeição da criação por produtor sem fazenda vinculada. */
    @Test
    @DisplayName("Deve lançar exceção 400 quando Produtor Rural tentar criar registro sem fazenda vinculada")
    void deveLancarExcecao400QuandoProdutorSemFazendaVinculada() {
        EnergyRegistryRequestDTO requestSemFarm = new EnergyRegistryRequestDTO(
                LocalDate.now(),
                new BigDecimal("300.00"),
                null
        );
        FarmOwner ownerSemFazenda = FarmOwner.builder().id(3L).idFarm(null).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(ownerSemFazenda));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.createEnergyRegistry(requestSemFarm, ownerPrincipal)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Produtor rural logado não possui fazenda vinculada"));
    }

    /** Verifica a rejeição da criação em outra fazenda por um produtor rural. */
    @Test
    @DisplayName("Deve lançar exceção 403 quando Produtor Rural tentar criar registro para outra fazenda")
    void deveLancarExcecao403QuandoProdutorTentarCriarEmOutraFazenda() {
        EnergyRegistryRequestDTO requestOutraFazenda = new EnergyRegistryRequestDTO(
                LocalDate.now(),
                new BigDecimal("300.00"),
                99L
        );
        Farm outraFazenda = Farm.builder().id(99L).idEnterprise(50L).build();
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();

        when(farmRepository.findById(99L)).thenReturn(Optional.of(outraFazenda));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.createEnergyRegistry(requestOutraFazenda, ownerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /** Verifica a rejeição da listagem para um perfil desconhecido. */
    @Test
    @DisplayName("Deve lançar exceção 403 quando perfil for desconhecido ao listar")
    void deveLancarExcecao403QuandoPerfilDesconhecidoListar() {
        UserPrincipal perfilInvalido = new UserPrincipal(9L, "outro@ouros.com", null, "GUEST", List.of());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.getEnergyRegistriesForUser(null, perfilInvalido)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /** Verifica a rejeição da exclusão por funcionário de outra empresa. */
    @Test
    @DisplayName("Deve lançar exceção 403 quando funcionário de outra empresa tentar deletar registro")
    void deveLancarExcecao403QuandoFuncionarioDeOutraEmpresaDeletar() {
        CompanyEmployee employeeDeOutraEmpresa = CompanyEmployee.builder().id(2L).idEnterprise(99L).build();
        when(energyRegistryRepository.findById(1L)).thenReturn(Optional.of(energyRegistry));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employeeDeOutraEmpresa));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.deleteEnergyRegistry(1L, employeePrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /** Verifica a exclusão bem-sucedida de um registro de energia. */
    @Test
    @DisplayName("Deve deletar registro de energia com sucesso")
    void deveDeletarRegistroDeEnergiaComSucesso() {
        when(energyRegistryRepository.findById(1L)).thenReturn(Optional.of(energyRegistry));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        assertDoesNotThrow(() -> energyRegistryService.deleteEnergyRegistry(1L, admPrincipal));

        verify(energyRegistryRepository, times(1)).delete(energyRegistry);
    }

    /** Verifica a atualização bem-sucedida de data e consumo do registro. */
    @Test
    @DisplayName("Deve atualizar registro de energia com sucesso")
    void deveAtualizarRegistroDeEnergiaComSucesso() {
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(
                LocalDate.of(2026, 9, 10),
                new BigDecimal("520.00")
        );

        when(energyRegistryRepository.findById(1L)).thenReturn(Optional.of(energyRegistry));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(energyRegistryRepository.save(any(EnergyRegistry.class))).thenReturn(energyRegistry);

        EnergyRegistryResponseDTO response = energyRegistryService.updateEnergyRegistry(1L, updateDTO, admPrincipal);

        assertNotNull(response);
        verify(energyRegistryRepository, times(1)).save(energyRegistry);
        assertEquals(LocalDate.of(2026, 9, 10), energyRegistry.getRegistrationDate());
        assertEquals(new BigDecimal("520.00"), energyRegistry.getEnergyConsumption());
    }

    /** Verifica que uma atualização vazia não persiste alterações. */
    @Test
    @DisplayName("Deve retornar registro sem salvar quando hasUpdates for false")
    void deveRetornarRegistroSemSalvarQuandoSemUpdates() {
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(null, null);

        when(energyRegistryRepository.findById(1L)).thenReturn(Optional.of(energyRegistry));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        EnergyRegistryResponseDTO response = energyRegistryService.updateEnergyRegistry(1L, updateDTO, admPrincipal);

        assertNotNull(response);
        verify(energyRegistryRepository, never()).save(any());
    }

    /** Verifica a rejeição da atualização sem usuário autenticado. */
    @Test
    @DisplayName("Deve lançar exceção 401 quando usuário não estiver autenticado ao atualizar")
    void deveLancarExcecao401QuandoNaoAutenticadoAoAtualizar() {
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(LocalDate.now(), null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.updateEnergyRegistry(1L, updateDTO, null)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    /** Verifica a rejeição da atualização quando o registro não existe. */
    @Test
    @DisplayName("Deve lançar exceção 404 quando registro não for encontrado ao atualizar")
    void deveLancarExcecao404QuandoRegistroNaoEncontradoAoAtualizar() {
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(LocalDate.now(), null);
        when(energyRegistryRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.updateEnergyRegistry(99L, updateDTO, admPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /** Verifica a rejeição da atualização por produtor de outra fazenda. */
    @Test
    @DisplayName("Deve lançar exceção 403 quando Produtor Rural tentar atualizar registro de outra fazenda")
    void deveLancarExcecao403QuandoProdutorTentarAtualizarOutraFazenda() {
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(LocalDate.now(), null);
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(99L).build(); // fazenda diferente (99 != 10)

        when(energyRegistryRepository.findById(1L)).thenReturn(Optional.of(energyRegistry));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.updateEnergyRegistry(1L, updateDTO, ownerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /** Verifica o tratamento de conflito de integridade durante a atualização. */
    @Test
    @DisplayName("Deve lançar exceção 409 quando ocorrer DataIntegrityViolationException ao atualizar")
    void deveLancarExcecao409QuandoErroIntegridadeAoAtualizar() {
        EnergyRegistryUpdateDTO updateDTO = new EnergyRegistryUpdateDTO(LocalDate.now(), new BigDecimal("600.00"));

        when(energyRegistryRepository.findById(1L)).thenReturn(Optional.of(energyRegistry));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(energyRegistryRepository.save(any(EnergyRegistry.class)))
                .thenThrow(new DataIntegrityViolationException("Erro de integridade"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> energyRegistryService.updateEnergyRegistry(1L, updateDTO, admPrincipal)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
