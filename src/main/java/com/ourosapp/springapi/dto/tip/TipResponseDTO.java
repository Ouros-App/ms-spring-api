package com.ourosapp.springapi.dto.tip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Tip;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de resposta contendo os dados de uma Dica Técnica e métricas agregadas de avaliações.
 *
 * @param id            Identificador único da dica
 * @param tip           Texto descritivo da dica técnica
 * @param idFarm        Identificador da fazenda vinculada
 * @param categories    Lista de nomes das categorias vinculadas
 * @param totalReviews  Total de avaliações recebidas pela dica
 * @param averageRating Média das notas de avaliação (0.0 a 5.0)
 */
@Schema(description = "Dados de resposta de uma dica técnica operacional")
public record TipResponseDTO(

        @Schema(description = "Identificador único da dica", example = "1")
        Long id,

        @Schema(description = "Texto descritivo da dica técnica", example = "Manter a ventilação mínima no galpão.")
        String tip,

        @Schema(description = "Identificador da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        Long idFarm,

        @Schema(description = "Lista de nomes das categorias vinculadas", example = "[\"Manejo de Ambiência\", \"Biosseguridade\"]")
        List<String> categories,

        @Schema(description = "Total de avaliações recebidas pela dica", example = "5")
        @JsonProperty("total_reviews")
        @JsonAlias("totalReviews")
        long totalReviews,

        @Schema(description = "Média de notas de avaliação da dica", example = "4.5")
        @JsonProperty("average_rating")
        @JsonAlias("averageRating")
        double averageRating
) {
    /**
     * Constrói um {@link TipResponseDTO} a partir da entidade {@link Tip} e dados agregados.
     *
     * @param entity        entidade JPA da dica
     * @param categories    lista de nomes das categorias
     * @param totalReviews  total de avaliações
     * @param averageRating média de avaliação
     * @return DTO de resposta preenchido
     */
    public static TipResponseDTO fromEntity(Tip entity, List<String> categories, long totalReviews, double averageRating) {
        if (entity == null) {
            return null;
        }
        return new TipResponseDTO(
                entity.getId(),
                entity.getTip(),
                entity.getIdFarm(),
                categories != null ? categories : List.of(),
                totalReviews,
                averageRating
        );
    }
}
