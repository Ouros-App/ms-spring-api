package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.lot.LotRequestDTO;
import com.ourosapp.springapi.dto.lot.LotResponseDTO;
import com.ourosapp.springapi.dto.lot.LotUpdateDTO;
import com.ourosapp.springapi.dto.lot.ProductionRegistryResponseDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.Lot;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.LotRepository;
import com.ourosapp.springapi.repository.projection.ProductionRegistryProjection;
import com.ourosapp.springapi.security.UserPrincipal;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link LotService}.
 */
@ExtendWith(MockitoExtension.class)
class LotServiceTest {

    @Mock
    private WaterRegistryService waterRegistryService;

    @Mock
    private LotRepository lotRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private LotService lotService;

    private final UserPrincipal admPrincipal = new UserPrincipal(
            1L, "adm@ouros.com", "pass", "ADM", List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    private final UserPrincipal employeePrincipal = new UserPrincipal(
            2L, "employee@empresa.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
    );

    private final UserPrincipal ownerPrincipal = new UserPrincipal(
            3L, "owner@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
    );

    private final UserPrincipal unknownPrincipal = new UserPrincipal(
            4L, "other@user.com", "pass", "UNKNOWN_ROLE", List.of()
    );

    // ==========================================
    // CREATE LOT
    // ==========================================

    @Test
    @DisplayName("Deve cadastrar lote com sucesso quando usuário for ADM")
    void deveCadastrarLoteComSucessoAdm() {
        LotRequestDTO request = new LotRequestDTO(
                50000, 48000, LocalDate.of(2026, 10, 15),
                2000, 15000.0, 1L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        Lot savedLot = Lot.builder()
                .id(100L)
                .receivedChickens(50000)
                .deliveredChickens(48000)
                .deliveryDate(LocalDate.of(2026, 10, 15))
                .losts(2000)
                .cost(15000.0)
                .idEnterprise(1L)
                .idFarm(10L)
                .build();

        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(lotRepository.save(any(Lot.class))).thenReturn(savedLot);

        LotResponseDTO response = lotService.createLot(request, admPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(50000, response.receivedChickens());
        assertEquals(48000, response.deliveredChickens());
        assertEquals(1L, response.idEnterprise());
        assertEquals(10L, response.idFarm());
        verify(lotRepository, times(1)).save(any(Lot.class));
    }

    @Test
    @DisplayName("Deve cadastrar lote com sucesso aplicando valores padrão quando campos de fechamento forem nulos")
    void deveCadastrarLoteComSucessoAplicandoDefaults() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, 1L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();

        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> {
            Lot arg = invocation.getArgument(0);
            arg.setId(101L);
            return arg;
        });

        LotResponseDTO response = lotService.createLot(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(101L, response.id());
        assertEquals(0, response.deliveredChickens());
        assertEquals(LocalDate.of(2026, 10, 15), response.deliveryDate());
        assertEquals(0, response.losts());
        assertEquals(0.0, response.cost());
    }

    @Test
    @DisplayName("Deve cadastrar lote com sucesso inferindo id_enterprise a partir do funcionário autenticado quando omitido")
    void deveCadastrarLoteComSucessoInferindoEmpresaDoFuncionario() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, null, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();

        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> {
            Lot arg = invocation.getArgument(0);
            arg.setId(102L);
            return arg;
        });

        LotResponseDTO response = lotService.createLot(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(102L, response.id());
        assertEquals(1L, response.idEnterprise());
        assertEquals(10L, response.idFarm());
    }

    @Test
    @DisplayName("Deve cadastrar lote com sucesso inferindo id_enterprise a partir da fazenda quando ADM omitir")
    void deveCadastrarLoteComSucessoInferindoEmpresaDaFazendaQuandoAdm() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, null, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();

        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> {
            Lot arg = invocation.getArgument(0);
            arg.setId(103L);
            return arg;
        });

        LotResponseDTO response = lotService.createLot(request, admPrincipal);

        assertNotNull(response);
        assertEquals(103L, response.id());
        assertEquals(1L, response.idEnterprise());
        assertEquals(10L, response.idFarm());
    }

    @Test
    @DisplayName("Deve lançar 404 quando empresa integradora não existir")
    void deveLancar404QuandoEmpresaNaoExistirAoCriarLote() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, 99L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(99L).build();
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(enterpriseRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.createLot(request, admPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Empresa integradora não encontrada"));
    }

    @Test
    @DisplayName("Deve lançar 404 quando fazenda não existir ao criar lote")
    void deveLancar404QuandoFazendaNaoExistirAoCriarLote() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, 1L, 99L
        );
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.createLot(request, admPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Fazenda não encontrada"));
    }

    @Test
    @DisplayName("Deve lançar 400 quando fazenda pertencer a outra empresa ao criar lote")
    void deveLancar400QuandoFazendaPertencerAOutraEmpresa() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, 1L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(2L).build();

        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(enterpriseRepository.existsById(1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.createLot(request, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("A fazenda informada não pertence"));
    }

    @Test
    @DisplayName("Deve lançar 403 quando funcionário tentar criar lote para outra empresa")
    void deveLancar403QuandoFuncionarioCriarLoteParaOutraEmpresa() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, 2L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(2L).build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();

        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.createLot(request, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve cadastrar lote com sucesso quando produtor rural cadastrar para sua própria fazenda")
    void deveCadastrarLoteComSucessoFarmOwnerParaSuaPropriaFazenda() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, null,
                null, null, null, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();

        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> {
            Lot arg = invocation.getArgument(0);
            arg.setId(105L);
            return arg;
        });

        LotResponseDTO response = lotService.createLot(request, ownerPrincipal);

        assertNotNull(response);
        assertEquals(105L, response.id());
        assertEquals(1L, response.idEnterprise());
        assertEquals(10L, response.idFarm());
        assertNull(response.deliveryDate());
    }

    @Test
    @DisplayName("Deve lançar 403 quando produtor rural tentar criar lote para outra fazenda")
    void deveLancar403QuandoProdutorRuralTentarCriarLoteParaOutraFazenda() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, 1L, 20L
        );
        Farm farm = Farm.builder().id(20L).idEnterprise(1L).build();
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();

        when(farmRepository.findById(20L)).thenReturn(Optional.of(farm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.createLot(request, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("outra fazenda"));
    }

    @Test
    @DisplayName("Deve lançar 403 quando produtor rural tentar vincular lote a outra empresa")
    void deveLancar403QuandoProdutorRuralTentarVincularLoteAOutraEmpresa() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, 2L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();

        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.createLot(request, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("outra empresa integradora"));
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao salvar lote")
    void deveLancar409QuandoOcorrerViolacaoDeIntegridade() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 10, 15),
                null, null, 1L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();

        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(lotRepository.save(any(Lot.class))).thenThrow(new DataIntegrityViolationException("Erro de FK"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.createLot(request, admPrincipal));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ==========================================
    // GET LOTS FOR USER
    // ==========================================

    @Test
    @DisplayName("Deve listar todos os lotes para ADM sem filtros")
    void deveListarTodosLotesParaAdm() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        when(lotRepository.findAll()).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(null, null, admPrincipal);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    @DisplayName("Deve listar lotes para ADM filtrando por idFarm e idEnterprise")
    void deveListarLotesParaAdmComFiltrosCompletos() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        when(lotRepository.findAllByIdEnterpriseAndIdFarm(1L, 10L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(10L, 1L, admPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar lotes para ADM filtrando por idEnterprise")
    void deveListarLotesParaAdmComFiltroEmpresa() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        when(lotRepository.findAllByIdEnterprise(1L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(null, 1L, admPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar lotes para ADM filtrando por idFarm")
    void deveListarLotesParaAdmComFiltroFazenda() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        when(lotRepository.findAllByIdFarm(10L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(10L, null, admPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar lotes para COMPANY_EMPLOYEE da sua empresa")
    void deveListarLotesParaFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(lotRepository.findAllByIdEnterprise(1L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(null, null, employeePrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar lotes para COMPANY_EMPLOYEE filtrando por fazenda da sua empresa")
    void deveListarLotesParaFuncionarioComFiltroFazendaValida() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(lotRepository.findAllByIdEnterpriseAndIdFarm(1L, 10L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(10L, null, employeePrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve lançar 403 quando COMPANY_EMPLOYEE filtrar por fazenda de outra integradora")
    void deveLancar403QuandoFuncionarioFiltrarPorFazendaDeOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Farm farm = Farm.builder().id(10L).idEnterprise(2L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotsForUser(10L, null, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 quando COMPANY_EMPLOYEE filtrar por outra idEnterprise")
    void deveLancar403QuandoFuncionarioFiltrarPorOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotsForUser(null, 2L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar lotes para FARM_OWNER da sua própria fazenda")
    void deveListarLotesParaProdutorRural() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(lotRepository.findAllByIdFarm(10L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(null, null, ownerPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve retornar lista vazia para FARM_OWNER sem fazenda cadastrada")
    void deveRetornarListaVaziaParaProdutorSemFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(null).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        List<LotResponseDTO> result = lotService.getLotsForUser(null, null, ownerPrincipal);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER filtrar por fazenda diferente da sua")
    void deveLancar403QuandoProdutorFiltrarPorOutraFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotsForUser(20L, null, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar lotes para FARM_OWNER filtrando por empresa integradora correspondente à sua fazenda")
    void deveListarLotesParaProdutorRuralComFiltroEmpresaValida() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(lotRepository.findAllByIdFarm(10L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(null, 1L, ownerPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER filtrar por empresa integradora diferente da sua fazenda")
    void deveLancar403QuandoProdutorFiltrarPorOutraEmpresa() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();

        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotsForUser(null, 2L, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Produtor rural não tem permissão para visualizar lotes de outra empresa integradora"));
    }

    @Test
    @DisplayName("Deve lançar 404 quando fazenda vinculada ao FARM_OWNER não existir ao filtrar por empresa integradora")
    void deveLancar404QuandoFazendaDoProdutorNaoExistirAoFiltrarPorEmpresa() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();

        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotsForUser(null, 1L, ownerPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Fazenda vinculada ao produtor rural não encontrada para o ID: 10"));
    }

    @Test
    @DisplayName("Deve listar lotes para FARM_OWNER com filtros de fazenda e empresa válidos")
    void deveListarLotesParaProdutorRuralComFiltrosValidos() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(lotRepository.findAllByIdFarm(10L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(10L, 1L, ownerPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve lançar 403 para perfil desconhecido na listagem")
    void deveLancar403ParaPerfilDesconhecidoNaListagem() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotsForUser(null, null, unknownPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ==========================================
    // GET LOT BY ID
    // ==========================================

    @Test
    @DisplayName("Deve buscar lote por ID com sucesso para ADM")
    void deveBuscarLotePorIdComSucessoAdm() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        LotResponseDTO response = lotService.getLotById(1L, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve buscar lote por ID com sucesso para COMPANY_EMPLOYEE da mesma empresa")
    void deveBuscarLotePorIdComSucessoFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        LotResponseDTO response = lotService.getLotById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve buscar lote por ID com sucesso para FARM_OWNER da mesma fazenda")
    void deveBuscarLotePorIdComSucessoProdutor() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        LotResponseDTO response = lotService.getLotById(1L, ownerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 404 quando lote não for encontrado por ID")
    void deveLancar404QuandoLoteNaoEncontrado() {
        when(lotRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotById(99L, admPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 quando ID for nulo")
    void deveLancar404QuandoIdForNulo() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotById(null, admPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 quando COMPANY_EMPLOYEE tentar acessar lote de outra integradora")
    void deveLancar403QuandoFuncionarioAcessarLoteDeOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(2L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotById(1L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar acessar lote de outra fazenda")
    void deveLancar403QuandoProdutorAcessarLoteDeOutraFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(20L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotById(1L, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ==========================================
    // UPDATE LOT (PATCH)
    // ==========================================

    @Test
    @DisplayName("Deve atualizar lote parcialmente com sucesso")
    void deveAtualizarLoteComSucesso() {
        Lot lot = Lot.builder()
                .id(1L)
                .receivedChickens(50000)
                .deliveredChickens(0)
                .deliveryDate(LocalDate.of(2026, 9, 1))
                .losts(0)
                .cost(0.0)
                .idEnterprise(1L)
                .idFarm(10L)
                .build();

        LotUpdateDTO updateDTO = new LotUpdateDTO(
                null, 49000, LocalDate.of(2026, 10, 15),
                1000, 18000.50
        );

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(lotRepository.save(any(Lot.class))).thenAnswer(i -> i.getArgument(0));

        LotResponseDTO response = lotService.updateLot(1L, updateDTO, admPrincipal);

        assertNotNull(response);
        assertEquals(49000, response.deliveredChickens());
        assertEquals(LocalDate.of(2026, 10, 15), response.deliveryDate());
        assertEquals(1000, response.losts());
        assertEquals(18000.50, response.cost());
    }

    @Test
    @DisplayName("Deve retornar lote original quando não houver atualizações no DTO")
    void deveRetornarOriginalQuandoSemAtualizacoes() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        LotUpdateDTO emptyUpdate = new LotUpdateDTO(null, null, null, null, null);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        LotResponseDTO response = lotService.updateLot(1L, emptyUpdate, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(lotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando deliveredChickens for maior que receivedChickens no update")
    void deveLancar400QuandoEntreguesMaiorQueRecebidasNoUpdate() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.of(2026, 10, 1)).losts(0).cost(0.0).build();
        LotUpdateDTO updateDTO = new LotUpdateDTO(null, 1500, null, null, null);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.updateLot(1L, updateDTO, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("aves entregues não pode ser superior"));
    }

    @Test
    @DisplayName("Deve atualizar lote com sucesso quando usuário for produtor rural da mesma fazenda")
    void deveAtualizarLoteComSucessoProdutorRuralSuaFazenda() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        LotUpdateDTO updateDTO = new LotUpdateDTO(null, 950, null, 50, 1200.0);
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(lotRepository.save(any(Lot.class))).thenAnswer(i -> i.getArgument(0));

        LotResponseDTO response = lotService.updateLot(1L, updateDTO, ownerPrincipal);

        assertNotNull(response);
        assertEquals(950, response.deliveredChickens());
        assertEquals(50, response.losts());
        assertEquals(1200.0, response.cost());
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar atualizar lote de outra fazenda")
    void deveLancar403QuandoProdutorAtualizarLoteDeOutraFazenda() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(20L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        LotUpdateDTO updateDTO = new LotUpdateDTO(null, 950, null, null, null);
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.updateLot(1L, updateDTO, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ==========================================
    // DELETE LOT
    // ==========================================

    @Test
    @DisplayName("Deve remover lote com sucesso quando usuário for ADM")
    void deveRemoverLoteComSucessoAdm() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        assertDoesNotThrow(() -> lotService.deleteLot(1L, admPrincipal));

        verify(lotRepository, times(1)).delete(lot);
        verify(lotRepository, times(1)).flush();
    }

    @Test
    @DisplayName("Deve remover lote com sucesso quando funcionário for da mesma empresa")
    void deveRemoverLoteComSucessoFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        assertDoesNotThrow(() -> lotService.deleteLot(1L, employeePrincipal));

        verify(lotRepository, times(1)).delete(lot);
    }

    @Test
    @DisplayName("Deve remover lote com sucesso quando produtor rural for da mesma fazenda")
    void deveRemoverLoteComSucessoProdutorRuralSuaFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        assertDoesNotThrow(() -> lotService.deleteLot(1L, ownerPrincipal));

        verify(lotRepository, times(1)).delete(lot);
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar remover lote de outra fazenda")
    void deveLancar403QuandoProdutorTentarRemoverLoteDeOutraFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(20L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.deleteLot(1L, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(lotRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar 409 quando houver restrição de integridade ao remover lote")
    void deveLancar409QuandoHouverConflitoAoRemoverLote() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).deliveryDate(LocalDate.now()).losts(0).cost(0.0).build();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        doThrow(new DataIntegrityViolationException("FK constraint")).when(lotRepository).flush();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.deleteLot(1L, admPrincipal));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 401 quando usuário não estiver autenticado")
    void deveLancar401QuandoNaoAutenticado() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getLotsForUser(null, null, null));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ==========================================
    // GET PRODUCTION SUMMARIES (BFF)
    // ==========================================

    private ProductionRegistryProjection createSampleProjection() {
        return new ProductionRegistryProjection() {
            @Override
            public Long getId() {
                return 10L;
            }

            @Override
            public String getIdLote() {
                return "L-10";
            }

            @Override
            public LocalDate getDataEmissao() {
                return LocalDate.of(2026, 8, 1);
            }

            @Override
            public Double getHidrometro() {
                return 7000.0;
            }

            @Override
            public Double getCusto() {
                return 6000.0;
            }

            @Override
            public Integer getEntraram() {
                return 6500;
            }

            @Override
            public Integer getSairam() {
                return 500;
            }

            @Override
            public Integer getGalinhasAtuais() {
                return 6000;
            }

            @Override
            public Long getIdFarm() {
                return 10L;
            }

            @Override
            public String getFarmName() {
                return "Granja São Judas";
            }
        };
    }

    @Test
    @DisplayName("Deve retornar resumo de produção para funcionário corporativo da sua empresa")
    void deveRetornarResumoDeProducaoParaFuncionarioCorporativo() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(lotRepository.findProductionSummaries(1L, null)).thenReturn(List.of(createSampleProjection()));

        List<ProductionRegistryResponseDTO> summaries = lotService.getProductionSummaries(null, null, employeePrincipal);

        assertEquals(1, summaries.size());
        ProductionRegistryResponseDTO dto = summaries.get(0);
        assertEquals(10L, dto.id());
        assertEquals("L-10", dto.idLote());
        assertEquals(LocalDate.of(2026, 8, 1), dto.dataEmissao());
        assertEquals(7000.0, dto.hidrometro());
        assertEquals(6000.0, dto.custo());
        assertEquals(6500, dto.entraram());
        assertEquals(500, dto.sairam());
        assertEquals(6000, dto.galinhasAtuais());
        assertEquals(10L, dto.idFarm());
        assertEquals("Granja São Judas", dto.farmName());
    }

    @Test
    @DisplayName("Deve permitir ADM consultar resumos de produção de qualquer empresa ou global")
    void devePermitirAdmFiltrarPorQualquerEmpresa() {
        when(lotRepository.findProductionSummaries(2L, 10L)).thenReturn(List.of(createSampleProjection()));

        List<ProductionRegistryResponseDTO> summaries = lotService.getProductionSummaries(10L, 2L, admPrincipal);

        assertEquals(1, summaries.size());
        verify(lotRepository, times(1)).findProductionSummaries(2L, 10L);
    }

    @Test
    @DisplayName("Deve bloquear funcionário consultando fazenda de outra empresa nos resumos de produção")
    void deveBloquearFuncionarioConsultandoFazendaDeOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Farm otherFarm = Farm.builder().id(20L).idEnterprise(99L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(otherFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getProductionSummaries(20L, null, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve bloquear funcionário tentando filtrar por outra empresa nos resumos de produção")
    void deveBloquearFuncionarioConsultandoOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getProductionSummaries(null, 99L, employeePrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve retornar resumos de produção para FARM_OWNER da sua própria fazenda")
    void deveRetornarResumoDeProducaoParaProdutorRural() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(lotRepository.findProductionSummaries(null, 10L)).thenReturn(List.of(createSampleProjection()));

        List<ProductionRegistryResponseDTO> summaries = lotService.getProductionSummaries(null, null, ownerPrincipal);

        assertEquals(1, summaries.size());
        assertEquals(10L, summaries.get(0).idFarm());
    }

    @Test
    @DisplayName("Deve bloquear FARM_OWNER consultando resumos de outra fazenda")
    void deveBloquearProdutorRuralConsultandoOutraFazenda() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getProductionSummaries(99L, null, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando produtor rural não possui fazenda vinculada")
    void deveRetornarListaVaziaQuandoProdutorNaoPossuiFazendaVinculada() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(null).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));

        List<ProductionRegistryResponseDTO> summaries = lotService.getProductionSummaries(null, null, ownerPrincipal);

        assertTrue(summaries.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar resumos de produção para FARM_OWNER filtrando por sua empresa com sucesso")
    void deveRetornarResumosParaProdutorRuralComEmpresaCorrespondente() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(lotRepository.findProductionSummaries(1L, 10L)).thenReturn(List.of(createSampleProjection()));

        List<ProductionRegistryResponseDTO> summaries = lotService.getProductionSummaries(null, 1L, ownerPrincipal);

        assertEquals(1, summaries.size());
        verify(lotRepository).findProductionSummaries(1L, 10L);
    }

    @Test
    @DisplayName("Deve bloquear FARM_OWNER tentando filtrar por empresa diferente da sua fazenda")
    void deveBloquearProdutorRuralFiltrandoEmpresaDiferente() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getProductionSummaries(null, 99L, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 quando fazenda do produtor não for encontrada")
    void deveLancar404QuandoFazendaDoProdutorNaoEncontrada() {
        FarmOwner owner = FarmOwner.builder().id(3L).idFarm(10L).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(farmRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getProductionSummaries(null, 1L, ownerPrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 para perfil desconhecido ao obter resumos de produção")
    void deveLancar403ParaPerfilDesconhecidoAoObterResumosProducao() {
        UserPrincipal otherPrincipal = new UserPrincipal(99L, "outro@ouros.com", "pass", "OTHER", List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getProductionSummaries(null, null, otherPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve permitir funcionário consultar resumos filtrando por fazenda da mesma empresa")
    void devePermitirFuncionarioConsultarFazendaDaMesmaEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(lotRepository.findProductionSummaries(1L, 10L)).thenReturn(List.of(createSampleProjection()));

        List<ProductionRegistryResponseDTO> summaries = lotService.getProductionSummaries(10L, 1L, employeePrincipal);

        assertEquals(1, summaries.size());
    }

    @Test
    @DisplayName("Deve lançar 404 quando funcionário consulta fazenda inexistente")
    void deveLancar404QuandoFuncionarioConsultaFazendaInexistente() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.getProductionSummaries(999L, null, employeePrincipal));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
    @Test
    void deveCadastrarLoteComLeituraInicial() {
        var reading = new java.math.BigDecimal("120.5000");
        var request = new com.ourosapp.springapi.dto.lot.LotAggregatedRequestDTO(100, 0, null, 1.0, 10L, 1L, reading);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(Farm.builder().id(10L).idEnterprise(1L).build()));
        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lotService.createAggregatedLot(request, admPrincipal);
        verify(waterRegistryService).createWaterRegistry(argThat(w -> w.idFarm().equals(10L)
                && w.startHydrometer().equals(reading) && w.endHydrometer().equals(reading)), eq(admPrincipal));
    }

    @Test
    void deveNegarResumoDeLoteDeOutraFazenda() {
        when(lotRepository.findById(100L)).thenReturn(Optional.of(Lot.builder().id(100L).idFarm(10L).build()));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(FarmOwner.builder().idFarm(20L).build()));
        var error = assertThrows(ResponseStatusException.class, () -> lotService.getProductionSummaryById(100L, ownerPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(lotRepository, never()).findProductionSummaryById(any(), any(), any());
    }

    @Test
    void deveConsultarResumoDoLoteAutorizado() {
        when(lotRepository.findById(100L)).thenReturn(Optional.of(Lot.builder().id(100L).idFarm(10L).idEnterprise(1L).build()));
        when(lotRepository.findProductionSummaryById(100L, 1L, 10L)).thenReturn(Optional.of(createSampleProjection()));
        assertNotNull(lotService.getProductionSummaryById(100L, admPrincipal));
    }
    @Test
    void deveOmitirMedicaoQuandoLeituraForZero() {
        var request = new com.ourosapp.springapi.dto.lot.LotAggregatedRequestDTO(100, 0, null, 1.0, 10L, 1L, java.math.BigDecimal.ZERO);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(Farm.builder().id(10L).idEnterprise(1L).build()));
        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lotService.createAggregatedLot(request, admPrincipal);
        verifyNoInteractions(waterRegistryService);
    }
    @Test
    void deveOmitirMedicaoQuandoLeituraNaoForInformada() {
        var request = new com.ourosapp.springapi.dto.lot.LotAggregatedRequestDTO(100, 0, null, 1.0, 10L, 1L, null);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(Farm.builder().id(10L).idEnterprise(1L).build()));
        when(enterpriseRepository.existsById(1L)).thenReturn(true);
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lotService.createAggregatedLot(request, admPrincipal);
        verifyNoInteractions(waterRegistryService);
    }
}
