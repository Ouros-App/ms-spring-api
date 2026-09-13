package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.lot.LotRequestDTO;
import com.ourosapp.springapi.dto.lot.LotResponseDTO;
import com.ourosapp.springapi.dto.lot.LotUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.Lot;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.LotRepository;
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

import java.math.BigDecimal;
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
                50000, 48000, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 15),
                new BigDecimal("2.8500"), 2000, 15000.0, 1L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();
        Lot savedLot = Lot.builder()
                .id(100L)
                .receivedChickens(50000)
                .deliveredChickens(48000)
                .dateBirth(LocalDate.of(2026, 9, 1))
                .deliveryDate(LocalDate.of(2026, 10, 15))
                .gain(new BigDecimal("2.8500"))
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
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 1L, 10L
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
        assertEquals(LocalDate.of(2026, 9, 1), response.deliveryDate());
        assertEquals(BigDecimal.ZERO, response.gain());
        assertEquals(0, response.losts());
        assertEquals(0.0, response.cost());
    }

    @Test
    @DisplayName("Deve cadastrar lote com sucesso inferindo id_enterprise a partir do funcionário autenticado quando omitido")
    void deveCadastrarLoteComSucessoInferindoEmpresaDoFuncionario() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, null, 10L
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
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, null, 10L
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
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 99L, 10L
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
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 1L, 99L
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
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 1L, 10L
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
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 2L, 10L
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
    @DisplayName("Deve lançar 403 quando produtor rural tentar criar lote")
    void deveLancar403QuandoProdutorRuralTentarCriarLote() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 1L, 10L
        );
        Farm farm = Farm.builder().id(10L).idEnterprise(1L).build();

        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.createLot(request, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao salvar lote")
    void deveLancar409QuandoOcorrerViolacaoDeIntegridade() {
        LotRequestDTO request = new LotRequestDTO(
                50000, null, LocalDate.of(2026, 9, 1), null,
                null, null, null, 1L, 10L
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
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        when(lotRepository.findAll()).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(null, null, admPrincipal);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    @DisplayName("Deve listar lotes para ADM filtrando por idFarm e idEnterprise")
    void deveListarLotesParaAdmComFiltrosCompletos() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        when(lotRepository.findAllByIdEnterpriseAndIdFarm(1L, 10L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(10L, 1L, admPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar lotes para ADM filtrando por idEnterprise")
    void deveListarLotesParaAdmComFiltroEmpresa() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        when(lotRepository.findAllByIdEnterprise(1L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(null, 1L, admPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar lotes para ADM filtrando por idFarm")
    void deveListarLotesParaAdmComFiltroFazenda() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        when(lotRepository.findAllByIdFarm(10L)).thenReturn(List.of(lot));

        List<LotResponseDTO> result = lotService.getLotsForUser(10L, null, admPrincipal);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve listar lotes para COMPANY_EMPLOYEE da sua empresa")
    void deveListarLotesParaFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();

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
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();

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
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();

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
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        LotResponseDTO response = lotService.getLotById(1L, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve buscar lote por ID com sucesso para COMPANY_EMPLOYEE da mesma empresa")
    void deveBuscarLotePorIdComSucessoFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();

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
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();

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
        Lot lot = Lot.builder().id(1L).idEnterprise(2L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();

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
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(20L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();

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
                .dateBirth(LocalDate.of(2026, 9, 1))
                .deliveryDate(LocalDate.of(2026, 9, 1))
                .gain(BigDecimal.ZERO)
                .losts(0)
                .cost(0.0)
                .idEnterprise(1L)
                .idFarm(10L)
                .build();

        LotUpdateDTO updateDTO = new LotUpdateDTO(
                null, 49000, null, LocalDate.of(2026, 10, 15),
                new BigDecimal("2.9500"), 1000, 18000.50
        );

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(lotRepository.save(any(Lot.class))).thenAnswer(i -> i.getArgument(0));

        LotResponseDTO response = lotService.updateLot(1L, updateDTO, admPrincipal);

        assertNotNull(response);
        assertEquals(49000, response.deliveredChickens());
        assertEquals(LocalDate.of(2026, 10, 15), response.deliveryDate());
        assertEquals(new BigDecimal("2.9500"), response.gain());
        assertEquals(1000, response.losts());
        assertEquals(18000.50, response.cost());
    }

    @Test
    @DisplayName("Deve retornar lote original quando não houver atualizações no DTO")
    void deveRetornarOriginalQuandoSemAtualizacoes() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        LotUpdateDTO emptyUpdate = new LotUpdateDTO(null, null, null, null, null, null, null);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        LotResponseDTO response = lotService.updateLot(1L, emptyUpdate, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(lotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando deliveredChickens for maior que receivedChickens no update")
    void deveLancar400QuandoEntreguesMaiorQueRecebidasNoUpdate() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.of(2026, 9, 1)).deliveryDate(LocalDate.of(2026, 10, 1)).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        LotUpdateDTO updateDTO = new LotUpdateDTO(null, 1500, null, null, null, null, null);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.updateLot(1L, updateDTO, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("aves entregues não pode ser superior"));
    }

    @Test
    @DisplayName("Deve lançar 400 quando deliveryDate for anterior a dateBirth no update")
    void deveLancar400QuandoDataEntregaAnteriorNascimentoNoUpdate() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.of(2026, 9, 1)).deliveryDate(LocalDate.of(2026, 10, 1)).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        LotUpdateDTO updateDTO = new LotUpdateDTO(null, null, null, LocalDate.of(2026, 8, 15), null, null, null);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.updateLot(1L, updateDTO, admPrincipal));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("data de entrega deve ser posterior"));
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar atualizar lote")
    void deveLancar403QuandoProdutorAtualizarLote() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        LotUpdateDTO updateDTO = new LotUpdateDTO(null, 950, null, null, null, null, null);

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

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
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        assertDoesNotThrow(() -> lotService.deleteLot(1L, admPrincipal));

        verify(lotRepository, times(1)).delete(lot);
        verify(lotRepository, times(1)).flush();
    }

    @Test
    @DisplayName("Deve remover lote com sucesso quando funcionário for da mesma empresa")
    void deveRemoverLoteComSucessoFuncionario() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(1L).build();
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();

        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        assertDoesNotThrow(() -> lotService.deleteLot(1L, employeePrincipal));

        verify(lotRepository, times(1)).delete(lot);
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar remover lote")
    void deveLancar403QuandoProdutorTentarRemoverLote() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                lotService.deleteLot(1L, ownerPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(lotRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar 409 quando houver restrição de integridade ao remover lote")
    void deveLancar409QuandoHouverConflitoAoRemoverLote() {
        Lot lot = Lot.builder().id(1L).idEnterprise(1L).idFarm(10L).receivedChickens(1000).deliveredChickens(900).dateBirth(LocalDate.now()).deliveryDate(LocalDate.now()).gain(BigDecimal.ZERO).losts(0).cost(0.0).build();
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
}
