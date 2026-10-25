package com.ourosapp.springapi.dto.tip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de requisição para cadastro de Dica Técnica (POST /tips).
 *
 * @param title       Título da dica técnica operacional (opcional, padrão "Dica Operacional")
 * @param tip         Conteúdo textual da dica técnica operacional (suporta Markdown)
 * @param imageUrl    URL da imagem no Cloudinary (opcional)
 * @param fotoUrl     URL legada da imagem para retrocompatibilidade (opcional)
 * @param idFarm      Identificador da fazenda vinculada
 * @param categoryIds Lista de identificadores das categorias associadas à dica
 */
@Schema(description = "Dados para cadastro de uma nova dica técnica operacional")
public record TipRequestDTO(

        @Schema(description = "Título da dica técnica operacional", example = "Regulagem Diária e Ajuste de Altura das Linhas de Nipple")
        @Size(max = 200, message = "O título da dica deve ter no máximo 200 caracteres")
        String title,

        @Schema(description = "Texto descritivo e instruções da dica técnica (suporta formatação Markdown)", example = "Manter a ventilação mínima no galpão durante a primeira semana.")
        @NotBlank(message = "O texto da dica é obrigatório")
        String tip,

        @Schema(description = "URL da imagem no Cloudinary", example = "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp")
        @JsonProperty("image_url")
        @JsonAlias({"imageUrl"})
        @Size(max = 2048, message = "A URL da imagem deve ter no máximo 2048 caracteres")
        String imageUrl,

        @Schema(description = "URL legada da imagem para retrocompatibilidade", example = "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp")
        @JsonProperty("foto_url")
        @JsonAlias({"fotoUrl"})
        @Size(max = 2048, message = "A URL da foto deve ter no máximo 2048 caracteres")
        String fotoUrl,

        @Schema(description = "Identificador da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @NotNull(message = "O ID da fazenda é obrigatório")
        @Positive(message = "O ID da fazenda deve ser maior que zero")
        Long idFarm,

        @Schema(description = "Lista de IDs das categorias associadas à dica", example = "[1, 2]")
        @JsonProperty("category_ids")
        @JsonAlias("categoryIds")
        List<Long> categoryIds
) {
    /**
     * Construtor compacto para sanitização de strings e definição de valores padrão.
     */
    public TipRequestDTO {
        title = (title != null && !title.isBlank()) ? title.trim() : "Dica Operacional";
        tip = tip != null ? tip.trim() : null;
        String effectiveImg = (imageUrl != null && !imageUrl.isBlank()) ? imageUrl.trim() :
                ((fotoUrl != null && !fotoUrl.isBlank()) ? fotoUrl.trim() : null);
        imageUrl = effectiveImg;
        fotoUrl = effectiveImg;
    }

    /**
     * Construtor de sobrecarga retrocompatível para chamadas que forneçam apenas o texto da dica.
     */
    public TipRequestDTO(String tip, Long idFarm, List<Long> categoryIds) {
        this("Dica Operacional", tip, null, null, idFarm, categoryIds);
    }

    /**
     * Construtor de sobrecarga retrocompatível para chamadas com título e foto únicos.
     */
    public TipRequestDTO(String title, String tip, String fotoUrl, Long idFarm, List<Long> categoryIds) {
        this(title, tip, fotoUrl, fotoUrl, idFarm, categoryIds);
    }
}
