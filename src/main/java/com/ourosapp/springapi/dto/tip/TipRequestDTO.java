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
 * @param tip         Conteúdo textual da dica técnica operacional
 * @param fotoUrl     URL do banner ou imagem ilustrativa da dica (opcional)
 * @param idFarm      Identificador da fazenda vinculada
 * @param categoryIds Lista de identificadores das categorias associadas à dica
 */
@Schema(description = "Dados para cadastro de uma nova dica técnica operacional")
public record TipRequestDTO(

        @Schema(description = "Título da dica técnica operacional", example = "Manejo de Ambiência no Primeiro Dia")
        @Size(max = 150, message = "O título da dica deve ter no máximo 150 caracteres")
        String title,

        @Schema(description = "Texto descritivo e instruções da dica técnica", example = "Manter a ventilação mínima no galpão durante a primeira semana.")
        @NotBlank(message = "O texto da dica é obrigatório")
        String tip,

        @Schema(description = "URL do banner ou foto ilustrativa da dica", example = "https://ouros.com/fotos/dica-ventilacao.jpg")
        @JsonProperty("foto_url")
        @JsonAlias({"fotoUrl", "photoUrl", "photo_url"})
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
        fotoUrl = (fotoUrl != null && !fotoUrl.isBlank()) ? fotoUrl.trim() : null;
    }

    /**
     * Construtor de sobrecarga retrocompatível para chamadas que forneçam apenas o texto da dica.
     */
    public TipRequestDTO(String tip, Long idFarm, List<Long> categoryIds) {
        this("Dica Operacional", tip, null, idFarm, categoryIds);
    }
}
