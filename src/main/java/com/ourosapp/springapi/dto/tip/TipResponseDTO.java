package com.ourosapp.springapi.dto.tip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Tip;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de resposta contendo os dados de uma Dica Técnica e métricas agregadas de avaliações.
 *
 * @param id            Identificador único da dica
 * @param title         Título da dica técnica operacional
 * @param tip           Texto descritivo da dica técnica
 * @param imageUrl      URL da imagem da dica no Cloudinary
 * @param fotoUrl       URL legada da imagem para retrocompatibilidade
 * @param idFarm        Identificador da fazenda vinculada
 * @param categories    Lista de nomes das categorias vinculadas
 * @param totalReviews  Total de avaliações recebidas pela dica
 * @param averageRating Média das notas de avaliação (0.0 a 5.0)
 * @param audienceMatch Indica se a dica pertence a uma audiência do produtor autenticado
 */
@Schema(description = "Dados de resposta de uma dica técnica operacional")
public record TipResponseDTO(

        @Schema(description = "Identificador único da dica", example = "1")
        Long id,

        @Schema(description = "Título da dica técnica operacional", example = "Manejo de Ambiência no Primeiro Dia")
        String title,

        @Schema(description = "Texto descritivo da dica técnica", example = "Manter a ventilação mínima no galpão.")
        String tip,

        @Schema(description = "URL da imagem no Cloudinary", example = "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp")
        @JsonProperty("image_url")
        @JsonAlias({"imageUrl"})
        String imageUrl,

        @Schema(description = "URL legada da imagem para retrocompatibilidade", example = "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp")
        @JsonProperty("foto_url")
        @JsonAlias({"fotoUrl"})
        String fotoUrl,

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
        double averageRating,

        @Schema(
                description = "Indica se a dica corresponde a alguma audiência do produtor autenticado; campo omitido quando não aplicável ou indisponível.",
                nullable = true
        )
        @JsonProperty("audience_match")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean audienceMatch
) {
    /**
     * Construtor de sobrecarga retrocompatível para instanciações sem título e foto explícitos.
     */
    public TipResponseDTO(Long id, String tip, Long idFarm, List<String> categories, long totalReviews, double averageRating) {
        this(id, "Dica Operacional", tip, null, null, idFarm, categories, totalReviews, averageRating, null);
    }

    /**
     * Construtor de sobrecarga para chamadas que forneçam apenas uma URL de imagem/foto.
     */
    public TipResponseDTO(Long id, String title, String tip, String fotoUrl, Long idFarm, List<String> categories, long totalReviews, double averageRating) {
        this(id, title, tip, fotoUrl, fotoUrl, idFarm, categories, totalReviews, averageRating, null);
    }

    /**
     * Construtor retrocompatível para chamadas que fornecem URLs de imagem e foto separadas.
     */
    public TipResponseDTO(
            Long id,
            String title,
            String tip,
            String imageUrl,
            String fotoUrl,
            Long idFarm,
            List<String> categories,
            long totalReviews,
            double averageRating
    ) {
        this(id, title, tip, imageUrl, fotoUrl, idFarm, categories, totalReviews, averageRating, null);
    }

    /**
     * Constrói um {@link TipResponseDTO} a partir da entidade {@link Tip}, fazenda vinculada e dados agregados.
     *
     * @param entity        entidade JPA da dica
     * @param idFarm        identificador da fazenda vinculada
     * @param categories    lista de nomes das categorias
     * @param totalReviews  total de avaliações
     * @param averageRating média de avaliação
     * @return DTO de resposta preenchido
     */
    public static TipResponseDTO fromEntity(Tip entity, Long idFarm, List<String> categories, long totalReviews, double averageRating) {
        if (entity == null) {
            return null;
        }
        String effectiveImage = entity.getImageUrl() != null ? entity.getImageUrl() : entity.getFotoUrl();
        return new TipResponseDTO(
                entity.getId(),
                entity.getTitle() != null ? entity.getTitle() : "Dica Operacional",
                entity.getTip(),
                effectiveImage,
                effectiveImage,
                idFarm,
                categories != null ? categories : List.of(),
                totalReviews,
                averageRating,
                null
        );
    }

    public TipResponseDTO withAudienceMatch(Boolean audienceMatch) {
        return new TipResponseDTO(
                id,
                title,
                tip,
                imageUrl,
                fotoUrl,
                idFarm,
                categories,
                totalReviews,
                averageRating,
                audienceMatch
        );
    }
}
