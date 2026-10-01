package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.tip.TipRequestDTO;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.dto.tip.TipUpdateDTO;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.repository.*;
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
 * Testes unitários para {@link TipService}.
 */
@ExtendWith(MockitoExtension.class)
class TipServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TipCategoryRepository tipCategoryRepository;

    @Mock
    private FarmTipRepository farmTipRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private TipService tipService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;

    private Farm sampleFarm;
    private Farm otherEnterpriseFarm;
    private CompanyEmployee sampleEmployee;
    private FarmOwner sampleFarmOwner;
    private Tip sampleTip;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(
                1L, "admin@ouros.com", "pass", "ADM", List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                2L, "employee@empresa.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        farmOwnerPrincipal = new UserPrincipal(
                3L, "owner@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );

        sampleFarm = Farm.builder()
                .id(10L)
                .name("Fazenda Primavera")
                .areaProperty(new BigDecimal("150"))
                .region("Sul")
                .poultryCapacity(50000)
                .place("Setor 1")
                .idAddress(1L)
                .idEnterprise(50L)
                .build();

        otherEnterpriseFarm = Farm.builder()
                .id(20L)
                .name("Fazenda Outra")
                .areaProperty(new BigDecimal("100"))
                .region("Norte")
                .poultryCapacity(20000)
                .place("Setor 2")
                .idAddress(2L)
                .idEnterprise(999L)
                .build();

        sampleEmployee = CompanyEmployee.builder()
                .id(2L)
                .name("Carlos Funcionário")
                .documentNumber("12345678901")
                .email("employee@empresa.com")
                .telephone("11988887777")
                .password("secret")
                .idEnterprise(50L)
                .build();

        sampleFarmOwner = FarmOwner.builder()
                .id(3L)
                .name("João Produtor")
                .documentNumber("98765432100")
                .email("owner@fazenda.com")
                .telephone("11977776666")
                .password("secret")
                .idFarm(10L)
                .build();

        sampleTip = Tip.builder()
                .id(100L)
                .tip("Manter os bicos dos nebulizadores limpos")
                .build();

        sampleCategory = Category.builder()
                .id(5L)
                .category("Ambiência")
                .build();
    }

    @Test
    @DisplayName("createTip - Deve cadastrar dica técnica com sucesso por ADM")
    void deveCadastrarDicaComSucessoPorAdm() {
        TipRequestDTO request = new TipRequestDTO("Manter ventilação mínima", 10L, List.of(5L));

        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(categoryRepository.findByIdIn(List.of(5L))).thenReturn(List.of(sampleCategory));
        when(tipRepository.callCreateTip("Manter ventilação mínima", 10L, 5L)).thenReturn(100L);

        TipResponseDTO response = tipService.createTip(request, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Manter ventilação mínima", response.tip());
        assertEquals(10L, response.idFarm());
        assertEquals(List.of("Ambiência"), response.categories());
        verify(tipRepository).callCreateTip("Manter ventilação mínima", 10L, 5L);
    }

    @Test
    @DisplayName("createTip - Deve cadastrar dica técnica com sucesso por COMPANY_EMPLOYEE da mesma empresa")
    void deveCadastrarDicaComSucessoPorCompanyEmployee() {
        TipRequestDTO request = new TipRequestDTO("Manter ventilação mínima", 10L, null);

        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(tipRepository.callCreateTip("Manter ventilação mínima", 10L, null)).thenReturn(100L);

        TipResponseDTO response = tipService.createTip(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(10L, response.idFarm());
        verify(tipRepository).callCreateTip("Manter ventilação mínima", 10L, null);
    }

    @Test
    @DisplayName("createTip - Deve lançar 403 Forbidden quando usuário for FARM_OWNER")
    void deveLancarForbiddenAoCriarDicaComoFarmOwner() {
        TipRequestDTO request = new TipRequestDTO("Dica", 10L, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.createTip(request, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createTip - Deve lançar 401 Unauthorized quando principal for nulo")
    void deveLancarUnauthorizedQuandoPrincipalNulo() {
        TipRequestDTO request = new TipRequestDTO("Dica", 10L, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.createTip(request, null)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("createTip - Deve lançar 404 Not Found quando fazenda informada não existir")
    void deveLancarNotFoundQuandoFazendaNaoExistir() {
        TipRequestDTO request = new TipRequestDTO("Dica", 999L, null);

        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.createTip(request, adminPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("createTip - Deve lançar 403 Forbidden quando COMPANY_EMPLOYEE tentar criar dica para fazenda de outra empresa")
    void deveLancarForbiddenQuandoFuncionarioDeOutraEmpresa() {
        TipRequestDTO request = new TipRequestDTO("Dica", 20L, null);

        when(farmRepository.findById(20L)).thenReturn(Optional.of(otherEnterpriseFarm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.createTip(request, employeePrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createTip - Deve lançar 404 Not Found quando categoria informada não existir")
    void deveLancarNotFoundQuandoCategoriaNaoExistir() {
        TipRequestDTO request = new TipRequestDTO("Dica", 10L, List.of(999L));

        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(categoryRepository.findByIdIn(List.of(999L))).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.createTip(request, adminPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("createTip - Deve propagar DataIntegrityViolationException em caso de violação de integridade")
    void deveLancarConflictEmCasoDeViolacaoDeIntegridade() {
        TipRequestDTO request = new TipRequestDTO("Dica", 10L, null);

        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(tipRepository.callCreateTip(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DataIntegrityViolationException.class, () ->
                tipService.createTip(request, adminPrincipal)
        );
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar dicas com métricas e categorias para ADM")
    void deveRetornarDicasParaAdm() {
        when(tipRepository.findAll()).thenReturn(List.of(sampleTip));
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                TipCategory.builder().id(1L).idTip(100L).idCategory(5L).build()
        ));
        when(categoryRepository.findByIdIn(List.of(5L))).thenReturn(List.of(sampleCategory));
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                Review.builder().id(1L).idTip(100L).comment("Ótimo").rating(5).build(),
                Review.builder().id(2L).idTip(100L).comment("Bom").rating(4).build()
        ));

        List<TipResponseDTO> tips = tipService.getTipsForUser(null, adminPrincipal);

        assertEquals(1, tips.size());
        TipResponseDTO tip = tips.get(0);
        assertEquals(100L, tip.id());
        assertEquals(10L, tip.idFarm());
        assertEquals(List.of("Ambiência"), tip.categories());
        assertEquals(2, tip.totalReviews());
        assertEquals(4.5, tip.averageRating());
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar dicas da empresa para COMPANY_EMPLOYEE")
    void deveRetornarDicasParaCompanyEmployee() {
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of(sampleFarm));
        when(farmTipRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipRepository.findAllById(List.of(100L))).thenReturn(List.of(sampleTip));
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        List<TipResponseDTO> tips = tipService.getTipsForUser(null, employeePrincipal);

        assertEquals(1, tips.size());
        assertEquals(100L, tips.get(0).id());
        assertEquals(10L, tips.get(0).idFarm());
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar dicas para FARM_OWNER")
    void deveRetornarDicasParaFarmOwner() {
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(sampleFarmOwner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(farmTipRepository.findByIdFarm(10L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipRepository.findAllById(List.of(100L))).thenReturn(List.of(sampleTip));
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        List<TipResponseDTO> tips = tipService.getTipsForUser(null, farmOwnerPrincipal);

        assertEquals(1, tips.size());
        assertEquals(100L, tips.get(0).id());
        assertEquals(10L, tips.get(0).idFarm());
    }

    @Test
    @DisplayName("getTipsForUser - Deve filtrar por fazenda com validação de permissão")
    void deveFiltrarPorFazendaComValidacao() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmTipRepository.findByIdFarm(10L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipRepository.findAllById(List.of(100L))).thenReturn(List.of(sampleTip));
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        List<TipResponseDTO> tips = tipService.getTipsForUser(10L, employeePrincipal);

        assertEquals(1, tips.size());
        assertEquals(100L, tips.get(0).id());
        assertEquals(10L, tips.get(0).idFarm());
    }

    @Test
    @DisplayName("getTipById - Deve buscar dica com sucesso")
    void deveBuscarDicaPorIdComSucesso() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        TipResponseDTO tip = tipService.getTipById(100L, adminPrincipal);

        assertNotNull(tip);
        assertEquals(100L, tip.id());
        assertEquals(10L, tip.idFarm());
    }

    @Test
    @DisplayName("getTipById - Deve lançar 404 quando dica não existir")
    void deveLancarNotFoundQuandoDicaNaoExistirAoBuscarPorId() {
        when(tipRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipById(999L, adminPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("updateTip - Deve atualizar texto e categorias com sucesso")
    void deveAtualizarDicaComSucesso() {
        TipUpdateDTO request = new TipUpdateDTO("Novo texto da dica", List.of(5L));

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(categoryRepository.findByIdIn(List.of(5L))).thenReturn(List.of(sampleCategory));
        when(tipRepository.save(any(Tip.class))).thenReturn(sampleTip);
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                TipCategory.builder().id(1L).idTip(100L).idCategory(5L).build()
        ));
        when(categoryRepository.findByIdIn(List.of(5L))).thenReturn(List.of(sampleCategory));
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        TipResponseDTO updated = tipService.updateTip(100L, request, adminPrincipal);

        assertNotNull(updated);
        assertEquals("Novo texto da dica", sampleTip.getTip());
        verify(tipCategoryRepository).deleteByIdTip(100L);
        verify(tipCategoryRepository).save(any(TipCategory.class));
    }

    @Test
    @DisplayName("deleteTip - Deve remover associações e dica com sucesso")
    void deveRemoverDicaComSucesso() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));

        tipService.deleteTip(100L, adminPrincipal);

        verify(tipCategoryRepository).deleteByIdTip(100L);
        verify(farmTipRepository).deleteByIdTip(100L);
        verify(reviewRepository).deleteByIdTip(100L);
        verify(tipRepository).delete(sampleTip);
    }

    // =========================================================================
    // TESTES ADICIONAIS DE COBERTURA DE BRANCHES E MÉTODOS
    // =========================================================================

    @Test
    @DisplayName("createTip - Deve lançar NullPointerException se request for nulo")
    void deveLancarNullPointerExceptionQuandoRequestNuloAoCriarDica() {
        assertThrows(NullPointerException.class, () -> tipService.createTip(null, adminPrincipal));
    }

    @Test
    @DisplayName("createTip - Deve lançar 404 Not Found quando ID da fazenda for nulo")
    void deveLancarNotFoundQuandoIdFazendaNuloAoCriarDica() {
        TipRequestDTO request = new TipRequestDTO("Dica", null, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.createTip(request, adminPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Fazenda não encontrada para o ID: null"));
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar lista vazia quando COMPANY_EMPLOYEE não possuir fazendas vinculadas")
    void deveRetornarListaVaziaQuandoCompanyEmployeeNaoTiverFazendas() {
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of());

        List<TipResponseDTO> result = tipService.getTipsForUser(null, employeePrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar lista vazia quando as fazendas da empresa não tiverem dicas")
    void deveRetornarListaVaziaQuandoFazendasDaEmpresaNaoTiveremDicas() {
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of(sampleFarm));
        when(farmTipRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of());

        List<TipResponseDTO> result = tipService.getTipsForUser(null, employeePrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar lista vazia quando FARM_OWNER não tiver idFarm associado")
    void deveRetornarListaVaziaQuandoFarmOwnerNaoTiverIdFarm() {
        FarmOwner ownerWithoutFarm = FarmOwner.builder().id(3L).idFarm(null).build();
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(ownerWithoutFarm));

        List<TipResponseDTO> result = tipService.getTipsForUser(null, farmOwnerPrincipal);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getTipsForUser - Deve lançar 403 Forbidden para perfil não autorizado")
    void deveLancarForbiddenAoListarDicasParaPerfilDesconhecido() {
        UserPrincipal guest = new UserPrincipal(99L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipsForUser(null, guest)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getTipsForUser - Deve lançar 403 Forbidden quando FARM_OWNER filtrar por fazenda que não é sua")
    void deveLancarForbiddenQuandoFarmOwnerFiltrarPorOutraFazenda() {
        when(farmRepository.findById(20L)).thenReturn(Optional.of(otherEnterpriseFarm));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(sampleFarmOwner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipsForUser(20L, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getTipsForUser - Deve lançar 403 Forbidden quando perfil desconhecido filtrar por fazenda")
    void deveLancarForbiddenQuandoPerfilDesconhecidoFiltrarPorFazenda() {
        UserPrincipal guest = new UserPrincipal(99L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipsForUser(10L, guest)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getTipById - Deve permitir COMPANY_EMPLOYEE visualizar dica sem vínculos de fazenda")
    void devePermitirCompanyEmployeeVisualizarDicaSemVinculoDeFazenda() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of());
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        TipResponseDTO tip = tipService.getTipById(100L, employeePrincipal);

        assertNotNull(tip);
        assertEquals(100L, tip.id());
    }

    @Test
    @DisplayName("getTipById - Deve lançar 403 Forbidden quando COMPANY_EMPLOYEE tentar visualizar dica de outra empresa")
    void deveLancarForbiddenQuandoCompanyEmployeeVisualizarDicaDeOutraEmpresa() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(20L).idTip(100L).build()
        ));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(otherEnterpriseFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipById(100L, employeePrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getTipById - Deve permitir FARM_OWNER visualizar dica de sua fazenda")
    void devePermitirFarmOwnerVisualizarDicaDeSuaFazenda() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(sampleFarmOwner));
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        TipResponseDTO tip = tipService.getTipById(100L, farmOwnerPrincipal);

        assertNotNull(tip);
        assertEquals(100L, tip.id());
    }

    @Test
    @DisplayName("getTipById - Deve lançar 403 Forbidden quando FARM_OWNER não tiver idFarm")
    void deveLancarForbiddenQuandoFarmOwnerNaoTiverIdFarmAoBuscarDica() {
        FarmOwner ownerWithoutFarm = FarmOwner.builder().id(3L).idFarm(null).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(ownerWithoutFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipById(100L, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getTipById - Deve lançar 403 Forbidden quando FARM_OWNER tentar visualizar dica de outra fazenda")
    void deveLancarForbiddenQuandoFarmOwnerVisualizarDicaDeOutraFazenda() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(20L).idTip(100L).build()
        ));
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(sampleFarmOwner));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipById(100L, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getTipById - Deve lançar 403 Forbidden quando perfil não autorizado buscar dica")
    void deveLancarForbiddenQuandoPerfilNaoAutorizadoBuscarDica() {
        UserPrincipal guest = new UserPrincipal(99L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipById(100L, guest)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("updateTip - Deve retornar dica inalterada quando updateDTO não tiver alterações")
    void deveRetornarDicaInalteradaQuandoNaoHouverAtualizacoes() {
        TipUpdateDTO emptyRequest = new TipUpdateDTO(null, null);

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of());
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        TipResponseDTO result = tipService.updateTip(100L, emptyRequest, adminPrincipal);

        assertNotNull(result);
        assertEquals(100L, result.id());
        verify(tipRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTip - Deve permitir COMPANY_EMPLOYEE atualizar dica de sua empresa")
    void devePermitirCompanyEmployeeAtualizarDicaDeSuaEmpresa() {
        TipUpdateDTO request = new TipUpdateDTO("Novo texto", null);

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build()
        ));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(tipRepository.save(any(Tip.class))).thenReturn(sampleTip);
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        TipResponseDTO result = tipService.updateTip(100L, request, employeePrincipal);

        assertNotNull(result);
        verify(tipRepository).save(any(Tip.class));
    }

    @Test
    @DisplayName("updateTip - Deve lançar 403 Forbidden quando FARM_OWNER tentar atualizar dica")
    void deveLancarForbiddenQuandoFarmOwnerTentarAtualizarDica() {
        TipUpdateDTO request = new TipUpdateDTO("Novo texto", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.updateTip(100L, request, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("updateTip - Deve lançar 403 Forbidden quando COMPANY_EMPLOYEE tentar atualizar dica de outra empresa")
    void deveLancarForbiddenQuandoCompanyEmployeeTentarAtualizarDicaDeOutraEmpresa() {
        TipUpdateDTO request = new TipUpdateDTO("Novo texto", null);

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(20L).idTip(100L).build()
        ));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(otherEnterpriseFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.updateTip(100L, request, employeePrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("updateTip - Deve propagar DataIntegrityViolationException quando ocorrer erro de integridade")
    void deveLancarConflictAoOcorrerViolacaoDeIntegridadeEmUpdateTip() {
        TipUpdateDTO request = new TipUpdateDTO("Novo texto", null);

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of());
        when(tipRepository.save(any(Tip.class))).thenThrow(new DataIntegrityViolationException("Erro"));

        assertThrows(DataIntegrityViolationException.class, () ->
                tipService.updateTip(100L, request, adminPrincipal)
        );
    }

    @Test
    @DisplayName("deleteTip - Deve lançar 403 Forbidden quando FARM_OWNER tentar remover dica")
    void deveLancarForbiddenQuandoFarmOwnerTentarRemoverDica() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.deleteTip(100L, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteTip - Deve lançar 403 Forbidden quando COMPANY_EMPLOYEE tentar remover dica de outra empresa")
    void deveLancarForbiddenQuandoCompanyEmployeeTentarRemoverDicaDeOutraEmpresa() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(20L).idTip(100L).build()
        ));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(otherEnterpriseFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.deleteTip(100L, employeePrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("deleteTip - Deve propagar DataIntegrityViolationException quando ocorrer erro de integridade ao remover")
    void deveLancarConflictQuandoOcorrerViolacaoDeIntegridadeAoRemoverDica() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        doThrow(new DataIntegrityViolationException("FK")).when(tipCategoryRepository).deleteByIdTip(100L);

        assertThrows(DataIntegrityViolationException.class, () ->
                tipService.deleteTip(100L, adminPrincipal)
        );
    }

    @Test
    @DisplayName("Helpers - Deve lançar 404 Not Found quando funcionário não for encontrado")
    void deveLancarNotFoundQuandoFuncionarioNaoEncontradoNoHelper() {
        when(companyEmployeeRepository.findById(99L)).thenReturn(Optional.empty());
        UserPrincipal principal = new UserPrincipal(99L, "emp@test.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipsForUser(null, principal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Funcionário logado não encontrado"));
    }

    @Test
    @DisplayName("Helpers - Deve lançar 404 Not Found quando produtor rural não for encontrado")
    void deveLancarNotFoundQuandoProdutorNaoEncontradoNoHelper() {
        when(farmOwnerRepository.findById(99L)).thenReturn(Optional.empty());
        UserPrincipal principal = new UserPrincipal(99L, "owner@test.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipsForUser(null, principal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Produtor rural logado não encontrado"));
    }

    @Test
    @DisplayName("Helpers - Deve lançar 404 Not Found quando ID da dica for nulo")
    void deveLancarNotFoundQuandoIdDicaNulo() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipById(null, adminPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Dica técnica não encontrada para o ID: null"));
    }

    @Test
    @DisplayName("createTip - Não deve duplicar TipCategory se a associação já existir")
    void deveNaoDuplicarTipCategorySeJaExistir() {
        Category secondCategory = Category.builder().id(6L).category("Manejo").build();
        TipRequestDTO request = new TipRequestDTO("Dica existente", 10L, List.of(5L, 6L));

        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(categoryRepository.findByIdIn(List.of(5L, 6L))).thenReturn(List.of(sampleCategory, secondCategory));
        when(tipRepository.callCreateTip("Dica existente", 10L, 5L)).thenReturn(100L);
        when(tipCategoryRepository.existsByIdTipAndIdCategory(100L, 6L)).thenReturn(true);

        TipResponseDTO response = tipService.createTip(request, adminPrincipal);

        assertNotNull(response);
        verify(tipCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTip - Deve ignorar texto em branco e atualizar apenas categorias")
    void deveIgnorarTextoEmBrancoAoAtualizarDica() {
        TipUpdateDTO request = new TipUpdateDTO("   ", List.of(5L));

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(categoryRepository.findByIdIn(List.of(5L))).thenReturn(List.of(sampleCategory));
        when(tipRepository.save(any(Tip.class))).thenReturn(sampleTip);
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        TipResponseDTO updated = tipService.updateTip(100L, request, adminPrincipal);

        assertNotNull(updated);
        verify(tipCategoryRepository).deleteByIdTip(100L);
        verify(tipCategoryRepository).save(any(TipCategory.class));
    }

    @Test
    @DisplayName("enrichTips - Deve lidar com categorias desvinculadas ou nulas no mapa")
    void deveLidarComCategoriasNulasNoMapaAoEnriquecerDicas() {
        when(tipRepository.findAll()).thenReturn(List.of(sampleTip));
        when(farmTipRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                FarmTip.builder().id(1L).idFarm(10L).idTip(100L).build(),
                FarmTip.builder().id(2L).idFarm(10L).idTip(100L).build()
        ));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                TipCategory.builder().id(1L).idTip(100L).idCategory(999L).build()
        ));
        when(categoryRepository.findByIdIn(List.of(999L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        List<TipResponseDTO> tips = tipService.getTipsForUser(null, adminPrincipal);

        assertEquals(1, tips.size());
        assertTrue(tips.get(0).categories().isEmpty());
    }

    @Test
    @DisplayName("ensureAuthenticated - Deve lançar 401 quando principal.getId() for nulo")
    void deveLancarUnauthorizedQuandoPrincipalIdNulo() {
        UserPrincipal principalWithoutId = new UserPrincipal(null, "email@test.com", "pass", "ADM", List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.getTipsForUser(null, principalWithoutId)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

}
