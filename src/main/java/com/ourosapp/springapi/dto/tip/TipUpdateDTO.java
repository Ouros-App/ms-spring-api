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
 * @param tip         Novo texto da dica técnica (opcional, suporta Markdown)
 * @param imageUrl    Nova URL da imagem no Cloudinary (opcional)
 * @param fotoUrl     Nova URL legada da imagem para retrocompatibilidade (opcional)
 * @param categoryIds Nova lista de identificadores de categorias vinculadas (opcional)
 */
@Schema(description = "Dados para atualização parcial de uma dica técnica")
public record TipUpdateDTO(

        @Schema(description = "Novo título da dica técnica operacional", example = "Regulagem Diária e Ajuste de Altura das Linhas de Nipple")
        @Size(max = 200, message = "O título da dica deve ter no máximo 200 caracteres")
        String title,

        @Schema(description = "Novo texto da dica técnica (suporta formatação Markdown)", example = "Ajustar os exaustores conforme o quadro de temperatura interna.")
        String tip,

        @Schema(description = "Nova URL da imagem no Cloudinary", example = "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp")
        @JsonProperty("image_url")
        @JsonAlias({"imageUrl", "foto_url", "fotoUrl"})
        @Size(max = 2048, message = "A URL da imagem deve ter no máximo 2048 caracteres")
        String imageUrl,

        @Schema(description = "Nova URL legada da imagem para retrocompatibilidade", example = "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp")
        @JsonProperty("foto_url")
        @JsonAlias({"fotoUrl", "image_url", "imageUrl"})
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
        String effectiveImg = imageUrl != null ? imageUrl.trim() : (fotoUrl != null ? fotoUrl.trim() : null);
        imageUrl = effectiveImg;
        fotoUrl = effectiveImg;
    }

    /**
     * Construtor de sobrecarga retrocompatível para chamadas que forneçam apenas texto e categorias.
     */
    public TipUpdateDTO(String tip, List<Long> categoryIds) {
        this(null, tip, null, null, categoryIds);
    }

    /**
     * Construtor de sobrecarga retrocompatível para chamadas com título e foto únicos.
     */
    public TipUpdateDTO(String title, String tip, String fotoUrl, List<Long> categoryIds) {
        this(title, tip, fotoUrl, fotoUrl, categoryIds);
    }

    /**
     * Verifica se ao menos um dos campos foi informado para atualização.
     *
     * @return {@code true} se houver título, texto, imagem/foto ou lista de categorias preenchidos, {@code false} caso contrário
     */
    public boolean hasUpdates() {
        return (title != null && !title.isBlank())
                || (tip != null && !tip.isBlank())
                || imageUrl != null
                || fotoUrl != null
                || categoryIds != null;
    }
}
