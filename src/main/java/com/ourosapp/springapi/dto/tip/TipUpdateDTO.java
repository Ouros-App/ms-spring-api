package com.ourosapp.springapi.dto.tip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de requisição para atualização parcial de uma Dica Técnica (PATCH /tips/{id}).
 *
 * @param title       Novo título da dica técnica operacional (opcional)
 * @param tip         Novo texto da dica técnica (opcional)
 * @param fotoUrl     Nova URL do banner ou imagem ilustrativa da dica (opcional)
 * @param categoryIds Nova lista de identificadores de categorias vinculadas (opcional)
 */
@Schema(description = "Dados para atualização parcial de uma dica técnica")
public record TipUpdateDTO(

        @Schema(description = "Novo título da dica técnica operacional", example = "Ajuste Fino de Ambiência")
        @Size(max = 150, message = "O título da dica deve ter no máximo 150 caracteres")
        String title,

        @Schema(description = "Novo texto da dica técnica", example = "Ajustar os exaustores conforme o quadro de temperatura interna.")
        String tip,

        @Schema(description = "Nova URL do banner ou foto ilustrativa da dica", example = "https://ouros.com/fotos/dica-nova.jpg")
        @JsonProperty("foto_url")
        @JsonAlias({"fotoUrl", "photoUrl", "photo_url"})
        @Size(max = 2048, message = "A URL da foto deve ter no máximo 2048 caracteres")
        String fotoUrl,

        @Schema(description = "Nova lista de IDs de categorias vinculadas", example = "[1, 3]")
        @JsonProperty("category_ids")
        @JsonAlias("categoryIds")
        List<Long> categoryIds
) {
    /**
     * Construtor compacto para sanitização de strings.
     */
    public TipUpdateDTO {
        title = title != null ? title.trim() : null;
        tip = tip != null ? tip.trim() : null;
        fotoUrl = fotoUrl != null ? fotoUrl.trim() : null;
    }

    /**
     * Construtor de sobrecarga retrocompatível para chamadas que forneçam apenas texto e categorias.
     */
    public TipUpdateDTO(String tip, List<Long> categoryIds) {
        this(null, tip, null, categoryIds);
    }

    /**
     * Verifica se ao menos um dos campos foi informado para atualização.
     *
     * @return {@code true} se houver título, texto, foto ou lista de categorias preenchidos, {@code false} caso contrário
     */
    public boolean hasUpdates() {
        return (title != null && !title.isBlank())
                || (tip != null && !tip.isBlank())
                || fotoUrl != null
                || categoryIds != null;
    }
}
