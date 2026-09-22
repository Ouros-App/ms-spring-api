package com.ourosapp.springapi.dto.category;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para cadastro de Categoria de Dica Técnica (POST /categories).
 *
 * @param category Nome da categoria temática
 * @param idTip    Identificador da dica técnica associada à categoria
 */
@Schema(description = "Dados para cadastro de uma nova categoria de dicas técnicas")
public record CategoryRequestDTO(

        @Schema(description = "Nome descritivo da categoria temática", example = "Manejo de Ambiência")
        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 50, message = "O nome da categoria deve ter no máximo 50 caracteres")
        String category,

        @Schema(description = "Identificador da dica técnica associada à categoria", example = "1")
        @JsonProperty("id_tip")
        @JsonAlias("idTip")
        @NotNull(message = "O ID da dica associada é obrigatório")
        @Positive(message = "O ID da dica deve ser maior que zero")
        Long idTip
) {
    /**
     * Construtor compacto para sanitização de strings.
     */
    public CategoryRequestDTO {
        category = category != null ? category.trim() : null;
    }
}
