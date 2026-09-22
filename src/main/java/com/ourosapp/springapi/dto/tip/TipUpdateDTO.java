package com.ourosapp.springapi.dto.tip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de requisição para atualização parcial de uma Dica Técnica (PATCH /tips/{id}).
 *
 * @param tip         Novo texto da dica técnica (opcional)
 * @param categoryIds Nova lista de identificadores de categorias vinculadas (opcional)
 */
@Schema(description = "Dados para atualização parcial de uma dica técnica")
public record TipUpdateDTO(

        @Schema(description = "Novo texto da dica técnica", example = "Ajustar os exaustores conforme o quadro de temperatura interna.")
        String tip,

        @Schema(description = "Nova lista de IDs de categorias vinculadas", example = "[1, 3]")
        @JsonProperty("category_ids")
        @JsonAlias("categoryIds")
        List<Long> categoryIds
) {
    /**
     * Construtor compacto para sanitização de strings.
     */
    public TipUpdateDTO {
        tip = tip != null ? tip.trim() : null;
    }

    /**
     * Verifica se ao menos um dos campos foi informado para atualização.
     *
     * @return {@code true} se houver texto ou lista de categorias preenchidos, {@code false} caso contrário
     */
    public boolean hasUpdates() {
        return (tip != null && !tip.isBlank()) || categoryIds != null;
    }
}
