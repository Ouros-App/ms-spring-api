package com.ourosapp.springapi.dto.review;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para atualização parcial de uma Avaliação de Dica Técnica (PATCH /reviews/{id}).
 * Todos os campos são opcionais, permitindo atualizar apenas o comentário, a nota ou ambos.
 *
 * @param comment Novo comentário descritivo sobre a dica (opcional)
 * @param rating  Nova nota de avaliação da dica (opcional, no intervalo de 0 a 5)
 */
@Schema(description = "Dados para atualização parcial de uma avaliação de dica técnica")
public record ReviewUpdateDTO(

        @Schema(description = "Novo comentário sobre a eficácia da dica", example = "Comentário atualizado após verificação nos lotes subsequentes.")
        @JsonProperty("comment")
        @JsonAlias("comment")
        @Size(min = 1, message = "O comentário não pode ser vazio")
        String comment,

        @Schema(description = "Nova nota de avaliação da dica técnica (intervalo de 0 a 5)", example = "4")
        @JsonProperty("rating")
        @JsonAlias("rating")
        @Min(value = 0, message = "A nota da avaliação deve ser maior ou igual a 0")
        @Max(value = 5, message = "A nota da avaliação deve ser menor ou igual a 5")
        Integer rating
) {

    /**
     * Construtor compacto para sanitização dos dados recebidos.
     */
    public ReviewUpdateDTO {
        comment = comment != null ? comment.trim() : null;
    }

    /**
     * Verifica se pelo menos um dos campos opcionais foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo para atualizar
     */
    public boolean hasUpdates() {
        return (comment != null && !comment.isBlank()) || rating != null;
    }
}
