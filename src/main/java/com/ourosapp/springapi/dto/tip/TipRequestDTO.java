package com.ourosapp.springapi.dto.tip;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * DTO de requisição para cadastro de Dica Técnica (POST /tips).
 *
 * @param tip         Conteúdo textual da dica técnica operacional
 * @param idFarm      Identificador da fazenda vinculada
 * @param categoryIds Lista de identificadores das categorias associadas à dica
 */
@Schema(description = "Dados para cadastro de uma nova dica técnica operacional")
public record TipRequestDTO(

        @Schema(description = "Texto descritivo e instruções da dica técnica", example = "Manter a ventilação mínima no galpão durante a primeira semana.")
        @NotBlank(message = "O texto da dica é obrigatório")
        String tip,

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
     * Construtor compacto para sanitização de strings.
     */
    public TipRequestDTO {
        tip = tip != null ? tip.trim() : null;
    }
}
