package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.review.ReviewRequestDTO;
import com.ourosapp.springapi.dto.review.ReviewResponseDTO;
import com.ourosapp.springapi.dto.review.ReviewUpdateDTO;
import com.ourosapp.springapi.entity.Review;
import com.ourosapp.springapi.entity.Tip;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para DTOs e entidades do módulo de Avaliações (Review).
 */
class ReviewDtoTest {

    private static Validator validator;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }

    // =========================================================================
    // ReviewRequestDTO TESTS
    // =========================================================================

    @Test
    @DisplayName("ReviewRequestDTO - Deve criar DTO válido e aplicar trim no comentário")
    void testReviewRequestDTOValid() {
        ReviewRequestDTO dto = new ReviewRequestDTO("  Excelente dica para ventilação  ", 5);

        assertEquals("Excelente dica para ventilação", dto.comment());
        assertEquals(5, dto.rating());

        Set<ConstraintViolation<ReviewRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Não deve haver violações de validação para payload válido");
    }

    @Test
    @DisplayName("ReviewRequestDTO - Deve invalidar quando comment for nulo ou em branco")
    void testReviewRequestDTOInvalidComment() {
        ReviewRequestDTO nullComment = new ReviewRequestDTO(null, 4);
        Set<ConstraintViolation<ReviewRequestDTO>> violationsNull = validator.validate(nullComment);
        assertFalse(violationsNull.isEmpty());

        ReviewRequestDTO blankComment = new ReviewRequestDTO("   ", 4);
        Set<ConstraintViolation<ReviewRequestDTO>> violationsBlank = validator.validate(blankComment);
        assertFalse(violationsBlank.isEmpty());
    }

    @Test
    @DisplayName("ReviewRequestDTO - Deve invalidar quando rating for nulo, menor que 0 ou maior que 5")
    void testReviewRequestDTOInvalidRating() {
        ReviewRequestDTO nullRating = new ReviewRequestDTO("Comentário válido", null);
        Set<ConstraintViolation<ReviewRequestDTO>> violationsNull = validator.validate(nullRating);
        assertFalse(violationsNull.isEmpty());

        ReviewRequestDTO negativeRating = new ReviewRequestDTO("Comentário válido", -1);
        Set<ConstraintViolation<ReviewRequestDTO>> violationsNeg = validator.validate(negativeRating);
        assertFalse(violationsNeg.isEmpty());

        ReviewRequestDTO overRating = new ReviewRequestDTO("Comentário válido", 6);
        Set<ConstraintViolation<ReviewRequestDTO>> violationsOver = validator.validate(overRating);
        assertFalse(violationsOver.isEmpty());
    }

    @Test
    @DisplayName("ReviewRequestDTO - Deve deserializar JSON corretamente")
    void testReviewRequestDTODeserialization() throws Exception {
        String json = """
                {
                    "comment": "Dica essencial",
                    "rating": 4
                }
                """;
        ReviewRequestDTO dto = objectMapper.readValue(json, ReviewRequestDTO.class);
        assertNotNull(dto);
        assertEquals("Dica essencial", dto.comment());
        assertEquals(4, dto.rating());
    }

    // =========================================================================
    // ReviewResponseDTO TESTS
    // =========================================================================

    @Test
    @DisplayName("ReviewResponseDTO - Deve converter de entidade JPA corretamente via fromEntity")
    void testReviewResponseDTOFromEntity() {
        Review entity = Review.builder()
                .id(1L)
                .comment("Reduziu custos")
                .rating(5)
                .idTip(10L)
                .build();

        ReviewResponseDTO dto = ReviewResponseDTO.fromEntity(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("Reduziu custos", dto.comment());
        assertEquals(5, dto.rating());
        assertEquals(10L, dto.idTip());
    }

    @Test
    @DisplayName("ReviewResponseDTO - fromEntity deve retornar null para entidade nula")
    void testReviewResponseDTOFromEntityNull() {
        assertNull(ReviewResponseDTO.fromEntity(null));
    }

    @Test
    @DisplayName("ReviewResponseDTO - Deve serializar em snake_case com id_tip")
    void testReviewResponseDTOSerialization() throws Exception {
        ReviewResponseDTO dto = new ReviewResponseDTO(1L, "Ótimo resultado", 5, 20L);
        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"comment\":\"Ótimo resultado\""));
        assertTrue(json.contains("\"rating\":5"));
        assertTrue(json.contains("\"id_tip\":20"));
    }

    // =========================================================================
    // ReviewUpdateDTO TESTS
    // =========================================================================

    @Test
    @DisplayName("ReviewUpdateDTO - Deve sanitizar comentário com trim e validar hasUpdates()")
    void testReviewUpdateDTO() {
        ReviewUpdateDTO dto = new ReviewUpdateDTO("  Novo comentário  ", 4);
        assertEquals("Novo comentário", dto.comment());
        assertEquals(4, dto.rating());
        assertTrue(dto.hasUpdates());

        ReviewUpdateDTO commentOnly = new ReviewUpdateDTO("Apenas comentário", null);
        assertTrue(commentOnly.hasUpdates());

        ReviewUpdateDTO ratingOnly = new ReviewUpdateDTO(null, 3);
        assertTrue(ratingOnly.hasUpdates());

        ReviewUpdateDTO empty = new ReviewUpdateDTO(null, null);
        assertFalse(empty.hasUpdates());

        ReviewUpdateDTO blankComment = new ReviewUpdateDTO("   ", null);
        assertFalse(blankComment.hasUpdates());
    }

    @Test
    @DisplayName("ReviewUpdateDTO - Deve invalidar notas fora do intervalo [0, 5]")
    void testReviewUpdateDTOInvalidRating() {
        ReviewUpdateDTO invalidLow = new ReviewUpdateDTO("Novo", -1);
        Set<ConstraintViolation<ReviewUpdateDTO>> violationsLow = validator.validate(invalidLow);
        assertFalse(violationsLow.isEmpty());

        ReviewUpdateDTO invalidHigh = new ReviewUpdateDTO("Novo", 6);
        Set<ConstraintViolation<ReviewUpdateDTO>> violationsHigh = validator.validate(invalidHigh);
        assertFalse(violationsHigh.isEmpty());
    }

    // =========================================================================
    // ENTITY TESTS
    // =========================================================================

    @Test
    @DisplayName("Review & Tip Entities - Deve cobrir builders, getters, setters e toString")
    void testEntities() {
        Tip tip = Tip.builder()
                .id(1L)
                .tip("Manter temperatura constante")
                .idFarm(10L)
                .build();

        assertEquals(1L, tip.getId());
        assertEquals("Manter temperatura constante", tip.getTip());
        assertEquals(10L, tip.getIdFarm());
        assertNotNull(tip.toString());

        Tip tipNoArgs = new Tip();
        tipNoArgs.setId(2L);
        tipNoArgs.setTip("Dica 2");
        tipNoArgs.setIdFarm(20L);
        assertEquals(2L, tipNoArgs.getId());
        assertEquals("Dica 2", tipNoArgs.getTip());
        assertEquals(20L, tipNoArgs.getIdFarm());

        Review review = Review.builder()
                .id(100L)
                .comment("Excelente")
                .rating(5)
                .idTip(1L)
                .build();

        assertEquals(100L, review.getId());
        assertEquals("Excelente", review.getComment());
        assertEquals(5, review.getRating());
        assertEquals(1L, review.getIdTip());
        assertNotNull(review.toString());

        Review reviewNoArgs = new Review();
        reviewNoArgs.setId(101L);
        reviewNoArgs.setComment("Bom");
        reviewNoArgs.setRating(4);
        reviewNoArgs.setIdTip(2L);
        assertEquals(101L, reviewNoArgs.getId());
        assertEquals("Bom", reviewNoArgs.getComment());
        assertEquals(4, reviewNoArgs.getRating());
        assertEquals(2L, reviewNoArgs.getIdTip());
    }
}
