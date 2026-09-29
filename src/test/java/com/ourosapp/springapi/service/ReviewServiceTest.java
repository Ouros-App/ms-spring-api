package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.review.ReviewRequestDTO;
import com.ourosapp.springapi.dto.review.ReviewResponseDTO;
import com.ourosapp.springapi.dto.review.ReviewUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.FarmTip;
import com.ourosapp.springapi.entity.Review;
import com.ourosapp.springapi.entity.Tip;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.FarmTipRepository;
import com.ourosapp.springapi.repository.ReviewRepository;
import com.ourosapp.springapi.repository.TipRepository;
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
 * Testes unitários para a camada de serviço {@link ReviewService}.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TipRepository tipRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @Mock
    private FarmTipRepository farmTipRepository;

    @InjectMocks
    private ReviewService reviewService;

    private Farm sampleFarm;
    private Tip sampleTip;
    private Review sampleReview;
    private ReviewRequestDTO sampleRequest;
    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;

    @BeforeEach
    void setUp() {
        sampleFarm = Farm.builder()
                .id(1L)
                .name("Fazenda Santa Luzia")
                .areaProperty(new BigDecimal("100.00"))
                .region("Centro-Oeste")
                .poultryCapacity(40000)
                .place("Gleba 2")
                .idAddress(5L)
                .idEnterprise(10L)
                .build();

        sampleTip = Tip.builder()
                .id(100L)
                .tip("Manter os bicos dos bebedouros alinhados à altura do dorso das aves.")
                .build();

        sampleReview = Review.builder()
                .id(50L)
                .comment("Excelente recomendação, evitou desperdício de água.")
                .rating(5)
                .idTip(100L)
                .build();

        sampleRequest = new ReviewRequestDTO(
                "Excelente recomendação, evitou desperdício de água.",
                5
        );

        adminPrincipal = new UserPrincipal(
                1L, "admin@ouros.com", "pass", "ADM", List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                10L, "employee@empresa.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        farmOwnerPrincipal = new UserPrincipal(
                20L, "owner@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }

    // =========================================================================
    // CREATE REVIEW TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve cadastrar avaliação com sucesso como ADM")
    void testCreateReviewAsAdmSuccess() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);

        ReviewResponseDTO response = reviewService.createReview(100L, sampleRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals(50L, response.id());
        assertEquals("Excelente recomendação, evitou desperdício de água.", response.comment());
        assertEquals(5, response.rating());
        assertEquals(100L, response.idTip());

        verify(tipRepository).findById(100L);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve cadastrar avaliação com sucesso como COMPANY_EMPLOYEE da mesma integradora")
    void testCreateReviewAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(10L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(FarmTip.builder().id(1L).idFarm(1L).idTip(100L).build()));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);

        ReviewResponseDTO response = reviewService.createReview(100L, sampleRequest, employeePrincipal);

        assertNotNull(response);
        assertEquals(50L, response.id());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar 403 quando COMPANY_EMPLOYEE tentar avaliar dica de outra empresa")
    void testCreateReviewAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(FarmTip.builder().id(1L).idFarm(1L).idTip(100L).build()));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar avaliação com sucesso como FARM_OWNER da própria fazenda")
    void testCreateReviewAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmTipRepository.existsByIdFarmAndIdTip(1L, 100L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);

        ReviewResponseDTO response = reviewService.createReview(100L, sampleRequest, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(50L, response.id());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tentar avaliar dica de outra fazenda")
    void testCreateReviewAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmTipRepository.existsByIdFarmAndIdTip(999L, 100L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 404 quando dica técnica não existir")
    void testCreateReviewTipNotFound() {
        when(tipRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(999L, sampleRequest, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando rating for inválido (< 0 ou > 5)")
    void testCreateReviewInvalidRatingBadRequest() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));

        ReviewRequestDTO invalidRequestHigh = new ReviewRequestDTO("Comentário", 10);
        ResponseStatusException exHigh = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, invalidRequestHigh, adminPrincipal)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exHigh.getStatusCode());

        ReviewRequestDTO invalidRequestNegative = new ReviewRequestDTO("Comentário", -1);
        ResponseStatusException exNegative = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, invalidRequestNegative, adminPrincipal)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exNegative.getStatusCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar avaliação com sucesso quando rating for nulo")
    void testCreateReviewNullRatingSuccess() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        Review reviewWithoutRating = Review.builder().id(51L).comment("Apenas comentário").rating(null).idTip(100L).build();
        when(reviewRepository.save(any(Review.class))).thenReturn(reviewWithoutRating);

        ReviewRequestDTO request = new ReviewRequestDTO("Apenas comentário", null);
        ReviewResponseDTO response = reviewService.createReview(100L, request, adminPrincipal);

        assertNotNull(response);
        assertEquals(51L, response.id());
        assertNull(response.rating());
    }

    @Test
    @DisplayName("Deve lançar 401 quando principal for nulo ao criar avaliação")
    void testCreateReviewUnauthorizedNullPrincipal() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 401 quando principal.getId() for nulo ao criar avaliação")
    void testCreateReviewUnauthorizedNullPrincipalId() {
        UserPrincipal principalWithoutId = new UserPrincipal(null, "email@test.com", "pass", "ADM", List.of());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, principalWithoutId)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 403 para role inválida ao criar avaliação")
    void testCreateReviewForbiddenRole() {
        UserPrincipal guest = new UserPrincipal(1L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, guest)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao salvar avaliação")
    void testCreateReviewDataIntegrityViolation() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(reviewRepository.save(any(Review.class)))
                .thenThrow(new DataIntegrityViolationException("FK constraint"));

        assertThrows(DataIntegrityViolationException.class, () ->
                reviewService.createReview(100L, sampleRequest, adminPrincipal)
        );
    }

    // =========================================================================
    // GET REVIEWS BY TIP ID TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve listar avaliações de uma dica como ADM")
    void testGetReviewsByTipIdAsAdmSuccess() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(reviewRepository.findByIdTip(100L)).thenReturn(List.of(sampleReview));

        List<ReviewResponseDTO> result = reviewService.getReviewsByTipId(100L, adminPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(50L, result.get(0).id());
        verify(reviewRepository).findByIdTip(100L);
    }

    @Test
    @DisplayName("Deve listar avaliações de uma dica como COMPANY_EMPLOYEE da mesma empresa")
    void testGetReviewsByTipIdAsCompanyEmployeeSuccess() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(10L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(FarmTip.builder().id(1L).idFarm(1L).idTip(100L).build()));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));
        when(reviewRepository.findByIdTip(100L)).thenReturn(List.of(sampleReview));

        List<ReviewResponseDTO> result = reviewService.getReviewsByTipId(100L, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve lançar 403 ao listar avaliações de dica de outra empresa como COMPANY_EMPLOYEE")
    void testGetReviewsByTipIdAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(FarmTip.builder().id(1L).idFarm(1L).idTip(100L).build()));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.getReviewsByTipId(100L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar avaliações de uma dica como FARM_OWNER da mesma fazenda")
    void testGetReviewsByTipIdAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmTipRepository.existsByIdFarmAndIdTip(1L, 100L)).thenReturn(true);
        when(reviewRepository.findByIdTip(100L)).thenReturn(List.of(sampleReview));

        List<ReviewResponseDTO> result = reviewService.getReviewsByTipId(100L, farmOwnerPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Deve lançar 403 ao listar avaliações de dica de outra fazenda como FARM_OWNER")
    void testGetReviewsByTipIdAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmTipRepository.existsByIdFarmAndIdTip(999L, 100L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.getReviewsByTipId(100L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 ao listar avaliações de dica inexistente")
    void testGetReviewsByTipIdNotFound() {
        when(tipRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.getReviewsByTipId(999L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // =========================================================================
    // GET REVIEW BY ID TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve buscar avaliação por ID com sucesso como ADM")
    void testGetReviewByIdAsAdmSuccess() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));

        ReviewResponseDTO response = reviewService.getReviewById(50L, adminPrincipal);

        assertNotNull(response);
        assertEquals(50L, response.id());
    }

    @Test
    @DisplayName("Deve buscar avaliação por ID como FARM_OWNER da mesma fazenda")
    void testGetReviewByIdAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmTipRepository.existsByIdFarmAndIdTip(1L, 100L)).thenReturn(true);

        ReviewResponseDTO response = reviewService.getReviewById(50L, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(50L, response.id());
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar avaliação por ID inexistente")
    void testGetReviewByIdNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.getReviewById(999L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 ao buscar avaliação com ID nulo")
    void testGetReviewByIdNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.getReviewById(null, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // =========================================================================
    // UPDATE REVIEW TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve atualizar avaliação parcialmente com sucesso como ADM")
    void testUpdateReviewAsAdmSuccess() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewUpdateDTO updateDTO = new ReviewUpdateDTO("Comentário revisado", 4);

        ReviewResponseDTO response = reviewService.updateReview(50L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals("Comentário revisado", response.comment());
        assertEquals(4, response.rating());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve retornar avaliação inalterada quando DTO não possuir atualizações")
    void testUpdateReviewNoUpdates() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));

        ReviewUpdateDTO emptyUpdate = new ReviewUpdateDTO(null, null);

        ReviewResponseDTO response = reviewService.updateReview(50L, emptyUpdate, adminPrincipal);

        assertNotNull(response);
        assertEquals(50L, response.id());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 ao atualizar avaliação com nota inválida (> 5 ou < 0)")
    void testUpdateReviewInvalidRating() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));

        ReviewUpdateDTO invalidUpdateHigh = new ReviewUpdateDTO(null, 10);
        ResponseStatusException exHigh = assertThrows(ResponseStatusException.class, () ->
                reviewService.updateReview(50L, invalidUpdateHigh, adminPrincipal)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exHigh.getStatusCode());

        ReviewUpdateDTO invalidUpdateNegative = new ReviewUpdateDTO(null, -1);
        ResponseStatusException exNegative = assertThrows(ResponseStatusException.class, () ->
                reviewService.updateReview(50L, invalidUpdateNegative, adminPrincipal)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exNegative.getStatusCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve ignorar comentário em branco e atualizar apenas o rating")
    void testUpdateReviewBlankComment() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewUpdateDTO updateDTO = new ReviewUpdateDTO("   ", 3);
        ReviewResponseDTO response = reviewService.updateReview(50L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals("Excelente recomendação, evitou desperdício de água.", response.comment());
        assertEquals(3, response.rating());
    }

    @Test
    @DisplayName("Deve lançar 403 ao atualizar avaliação de outra empresa como COMPANY_EMPLOYEE")
    void testUpdateReviewAsCompanyEmployeeDifferentEnterpriseForbidden() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(999L).build();
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of(FarmTip.builder().id(1L).idFarm(1L).idTip(100L).build()));
        when(farmRepository.findById(1L)).thenReturn(Optional.of(sampleFarm));

        ReviewUpdateDTO updateDTO = new ReviewUpdateDTO("Novo", 4);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.updateReview(50L, updateDTO, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao atualizar avaliação")
    void testUpdateReviewDataIntegrityViolation() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(reviewRepository.save(any(Review.class)))
                .thenThrow(new DataIntegrityViolationException("Erro de constraint"));

        ReviewUpdateDTO updateDTO = new ReviewUpdateDTO("Novo comentário", 4);

        assertThrows(DataIntegrityViolationException.class, () ->
                reviewService.updateReview(50L, updateDTO, adminPrincipal)
        );
    }

    // =========================================================================
    // DELETE REVIEW TESTS
    // =========================================================================

    @Test
    @DisplayName("Deve remover avaliação com sucesso como ADM")
    void testDeleteReviewAsAdmSuccess() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        doNothing().when(reviewRepository).delete(sampleReview);

        assertDoesNotThrow(() -> reviewService.deleteReview(50L, adminPrincipal));

        verify(reviewRepository).delete(sampleReview);
        verify(reviewRepository).flush();
    }

    @Test
    @DisplayName("Deve remover avaliação com sucesso como FARM_OWNER da mesma fazenda")
    void testDeleteReviewAsFarmOwnerSuccess() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(1L).build();
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmTipRepository.existsByIdFarmAndIdTip(1L, 100L)).thenReturn(true);
        doNothing().when(reviewRepository).delete(sampleReview);

        assertDoesNotThrow(() -> reviewService.deleteReview(50L, farmOwnerPrincipal));

        verify(reviewRepository).delete(sampleReview);
    }

    @Test
    @DisplayName("Deve lançar 403 ao remover avaliação de outra fazenda como FARM_OWNER")
    void testDeleteReviewAsFarmOwnerDifferentFarmForbidden() {
        FarmOwner owner = FarmOwner.builder().id(20L).idFarm(999L).build();
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(owner));
        when(farmTipRepository.existsByIdFarmAndIdTip(999L, 100L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.deleteReview(50L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar 404 ao remover avaliação inexistente")
    void testDeleteReviewNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.deleteReview(999L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar 409 quando ocorrer DataIntegrityViolationException ao remover avaliação")
    void testDeleteReviewDataIntegrityViolation() {
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(sampleReview));
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        doThrow(new DataIntegrityViolationException("Constraint violation"))
                .when(reviewRepository).delete(sampleReview);

        assertThrows(DataIntegrityViolationException.class, () ->
                reviewService.deleteReview(50L, adminPrincipal)
        );
    }

    @Test
    @DisplayName("Deve permitir acesso de COMPANY_EMPLOYEE quando dica não tiver farmTips vinculadas")
    void testValidateTipAccessCompanyEmployeeEmptyFarmTips() {
        CompanyEmployee employee = CompanyEmployee.builder().id(10L).idEnterprise(10L).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(farmTipRepository.findByIdTip(100L)).thenReturn(List.of());
        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);

        ReviewResponseDTO response = reviewService.createReview(100L, sampleRequest, employeePrincipal);

        assertNotNull(response);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar 404 quando COMPANY_EMPLOYEE não for encontrado no banco")
    void testValidateTipAccessCompanyEmployeeNotFound() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, employeePrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Funcionário logado não encontrado"));
    }

    @Test
    @DisplayName("Deve lançar 404 quando FARM_OWNER não for encontrado no banco")
    void testValidateTipAccessFarmOwnerNotFound() {
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Produtor rural logado não encontrado"));
    }

    @Test
    @DisplayName("Deve lançar 403 quando FARM_OWNER tiver idFarm nulo")
    void testValidateTipAccessFarmOwnerNullFarmId() {
        FarmOwner ownerWithoutFarm = FarmOwner.builder().id(20L).idFarm(null).build();
        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(farmOwnerRepository.findById(20L)).thenReturn(Optional.of(ownerWithoutFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(100L, sampleRequest, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar 404 quando ID da dica técnica for nulo")
    void testFindTipByIdOrThrowNullId() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reviewService.createReview(null, sampleRequest, adminPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Dica técnica não encontrada para o ID: null"));
    }
}
