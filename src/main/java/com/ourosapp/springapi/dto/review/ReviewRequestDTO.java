package com.ourosapp.springapi.dto.review;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requisição para cadastro de Avaliação de Dica Técnica (POST /tips/{id}/reviews).
 *
 * @param comment Comentário descritivo ou feedback operacional sobre a dica
 * @param rating  Nota de eficácia atribuída à dica (de 0 a 5)
 */
@Schema(description = "Dados para cadastro de uma nova avaliação de dica técnica")
public record ReviewRequestDTO(

        @Schema(description = "Comentário descritivo sobre a aplicação prática e eficácia da dica", example = "Ajuste na ventilação reduziu significativamente a mortalidade na primeira semana.")
        @JsonProperty("comment")
        @JsonAlias("comment")
        @NotBlank(message = "O comentário da avaliação é obrigatório")
        String comment,

        @Schema(description = "Nota de avaliação da dica técnica (intervalo de 0 a 5)", example = "5")
        @JsonProperty("rating")
        @JsonAlias("rating")
        @NotNull(message = "A nota da avaliação é obrigatória")
        @Min(value = 0, message = "A nota da avaliação deve ser maior ou igual a 0")
        @Max(value = 5, message = "A nota da avaliação deve ser menor ou igual a 5")
        Integer rating
) {

    /**
     * Construtor compacto para sanitização dos dados recebidos.
     */
    public ReviewRequestDTO {
        comment = comment != null ? comment.trim() : null;
    }
}
