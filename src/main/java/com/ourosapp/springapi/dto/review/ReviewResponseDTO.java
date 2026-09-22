package com.ourosapp.springapi.dto.review;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Review;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta contendo os dados detalhados de uma Avaliação de Dica Técnica.
 *
 * @param id      Identificador único da avaliação
 * @param comment Comentário descritivo ou feedback sobre a dica
 * @param rating  Nota de eficácia atribuída à dica (de 0 a 5)
 * @param idTip   Identificador único da dica técnica avaliada
 */
@Schema(description = "Dados detalhados de uma avaliação de dica técnica")
public record ReviewResponseDTO(

        @Schema(description = "Identificador único da avaliação", example = "1")
        @JsonProperty("id")
        Long id,

        @Schema(description = "Comentário descritivo sobre a dica", example = "Ajuste na ventilação reduziu significativamente a mortalidade na primeira semana.")
        @JsonProperty("comment")
        String comment,

        @Schema(description = "Nota de eficácia atribuída à dica (de 0 a 5)", example = "5")
        @JsonProperty("rating")
        Integer rating,

        @Schema(description = "Identificador único da dica técnica avaliada", example = "10")
        @JsonProperty("id_tip")
        @JsonAlias("idTip")
        Long idTip
) {

    /**
     * Converte uma entidade JPA {@link Review} em seu respectivo DTO de resposta.
     *
     * @param entity entidade JPA a ser convertida
     * @return DTO de resposta preenchido ou {@code null} se a entidade for nula
     */
    public static ReviewResponseDTO fromEntity(Review entity) {
        if (entity == null) {
            return null;
        }
        return new ReviewResponseDTO(
                entity.getId(),
                entity.getComment(),
                entity.getRating(),
                entity.getIdTip()
        );
    }
}
