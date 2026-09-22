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
                .name("Fazenda Bela Vista")
                .areaProperty(new BigDecimal("100.00"))
                .region("Sul")
                .poultryCapacity(40000)
                .place("Linha 5")
                .idAddress(1L)
                .idEnterprise(50L)
                .build();

        otherEnterpriseFarm = Farm.builder()
                .id(20L)
                .name("Fazenda Outra")
                .areaProperty(new BigDecimal("50.00"))
                .region("Norte")
                .poultryCapacity(20000)
                .place("Linha 1")
                .idAddress(2L)
                .idEnterprise(99L)
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
                .idFarm(10L)
                .build();

        sampleCategory = Category.builder()
                .id(5L)
                .category("Ambiência")
                .idTip(100L)
                .build();
    }

    @Test
    @DisplayName("createTip - Deve cadastrar dica técnica com sucesso por ADM")
    void deveCadastrarDicaComSucessoPorAdm() {
        TipRequestDTO request = new TipRequestDTO("Manter ventilação mínima", 10L, List.of(5L));

        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(categoryRepository.findByIdIn(List.of(5L))).thenReturn(List.of(sampleCategory));
        when(tipRepository.save(any(Tip.class))).thenReturn(sampleTip);
        when(farmTipRepository.existsByIdFarmAndIdTip(10L, 100L)).thenReturn(false);
        when(tipCategoryRepository.existsByIdTipAndIdCategory(100L, 5L)).thenReturn(false);

        TipResponseDTO response = tipService.createTip(request, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Manter os bicos dos nebulizadores limpos", response.tip());
        assertEquals(10L, response.idFarm());
        assertEquals(List.of("Ambiência"), response.categories());
        assertEquals(0, response.totalReviews());
        assertEquals(0.0, response.averageRating());

        verify(farmTipRepository).save(any(FarmTip.class));
        verify(tipCategoryRepository).save(any(TipCategory.class));
    }

    @Test
    @DisplayName("createTip - Deve cadastrar dica técnica com sucesso por COMPANY_EMPLOYEE")
    void deveCadastrarDicaComSucessoPorCompanyEmployee() {
        TipRequestDTO request = new TipRequestDTO("Manter ventilação mínima", 10L, null);

        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(tipRepository.save(any(Tip.class))).thenReturn(sampleTip);
        when(farmTipRepository.existsByIdFarmAndIdTip(10L, 100L)).thenReturn(true);

        TipResponseDTO response = tipService.createTip(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(100L, response.id());
        verify(tipCategoryRepository, never()).save(any(TipCategory.class));
    }

    @Test
    @DisplayName("createTip - Deve lançar 403 Forbidden para FARM_OWNER")
    void deveLancarForbiddenAoCriarDicaComoFarmOwner() {
        TipRequestDTO request = new TipRequestDTO("Dica", 10L, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tipService.createTip(request, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createTip - Deve lançar 403 Forbidden quando funcionário acessar fazenda de outra empresa")
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
    @DisplayName("createTip - Deve lançar 409 Conflict em caso de DataIntegrityViolationException")
    void deveLancarConflictEmCasoDeViolacaoDeIntegridade() {
        TipRequestDTO request = new TipRequestDTO("Dica", 10L, null);

        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(tipRepository.save(any(Tip.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DataIntegrityViolationException.class, () ->
                tipService.createTip(request, adminPrincipal)
        );
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar dicas com métricas e categorias para ADM")
    void deveRetornarDicasParaAdm() {
        when(tipRepository.findAll()).thenReturn(List.of(sampleTip));
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
        assertEquals(List.of("Ambiência"), tip.categories());
        assertEquals(2, tip.totalReviews());
        assertEquals(4.5, tip.averageRating());
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar dicas da empresa para COMPANY_EMPLOYEE")
    void deveRetornarDicasParaCompanyEmployee() {
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findAllByIdEnterprise(50L)).thenReturn(List.of(sampleFarm));
        when(tipRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of(sampleTip));
        when(farmTipRepository.findByIdFarmIn(List.of(10L))).thenReturn(List.of());
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        List<TipResponseDTO> tips = tipService.getTipsForUser(null, employeePrincipal);

        assertEquals(1, tips.size());
        assertEquals(100L, tips.get(0).id());
    }

    @Test
    @DisplayName("getTipsForUser - Deve retornar dicas para FARM_OWNER")
    void deveRetornarDicasParaFarmOwner() {
        when(farmOwnerRepository.findById(3L)).thenReturn(Optional.of(sampleFarmOwner));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(tipRepository.findByIdFarm(10L)).thenReturn(List.of(sampleTip));
        when(farmTipRepository.findByIdFarm(10L)).thenReturn(List.of());
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        List<TipResponseDTO> tips = tipService.getTipsForUser(null, farmOwnerPrincipal);

        assertEquals(1, tips.size());
        assertEquals(100L, tips.get(0).id());
    }

    @Test
    @DisplayName("getTipsForUser - Deve filtrar por fazenda com validação de permissão")
    void deveFiltrarPorFazendaComValidacao() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(tipRepository.findByIdFarm(10L)).thenReturn(List.of(sampleTip));
        when(farmTipRepository.findByIdFarm(10L)).thenReturn(List.of());
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        List<TipResponseDTO> tips = tipService.getTipsForUser(10L, employeePrincipal);

        assertEquals(1, tips.size());
        assertEquals(100L, tips.get(0).id());
    }

    @Test
    @DisplayName("getTipById - Deve buscar dica com sucesso")
    void deveBuscarDicaPorIdComSucesso() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());
        when(reviewRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of());

        TipResponseDTO tip = tipService.getTipById(100L, adminPrincipal);

        assertNotNull(tip);
        assertEquals(100L, tip.id());
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
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(categoryRepository.findByIdIn(List.of(5L))).thenReturn(List.of(sampleCategory));
        when(tipRepository.save(any(Tip.class))).thenReturn(sampleTip);
        when(tipCategoryRepository.findByIdTipIn(List.of(100L))).thenReturn(List.of(
                TipCategory.builder().id(1L).idTip(100L).idCategory(5L).build()
        ));
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
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        tipService.deleteTip(100L, adminPrincipal);

        verify(tipCategoryRepository).deleteByIdTip(100L);
        verify(farmTipRepository).deleteByIdTip(100L);
        verify(reviewRepository).deleteByIdTip(100L);
        verify(tipRepository).delete(sampleTip);
    }
}
