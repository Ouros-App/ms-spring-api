package com.ourosapp.springapi.dto.category;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta contendo os dados de uma Categoria temática de Dicas Técnicas.
 *
 * @param id       Identificador único da categoria
 * @param category Nome descritivo da categoria
 * @param idTip    Identificador da dica técnica vinculada
 */
@Schema(description = "Dados de resposta de uma categoria de dicas técnicas")
public record CategoryResponseDTO(

        @Schema(description = "Identificador único da categoria", example = "1")
        Long id,

        @Schema(description = "Nome descritivo da categoria", example = "Manejo de Ambiência")
        String category
) {
    /**
     * Constrói um {@link CategoryResponseDTO} a partir de uma entidade {@link Category}.
     *
     * @param entity entidade JPA da categoria
     * @return DTO de resposta preenchido
     */
    public static CategoryResponseDTO fromEntity(Category entity) {
        if (entity == null) {
            return null;
        }
        return new CategoryResponseDTO(
                entity.getId(),
                entity.getCategory()
        );
    }
}
