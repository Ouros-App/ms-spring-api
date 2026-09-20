package com.ourosapp.springapi.dto.individualgoal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de requisição para cadastro de Meta Individual (POST /individual-goals).
 *
 * @param title       Título descritivo da meta individual (máx. 50 caracteres)
 * @param description Detalhamento ou observações da meta
 * @param type        Tipo ou categoria da meta (máx. 50 caracteres)
 * @param status      Status inicial da meta (máx. 50 caracteres)
 * @param targetValue Valor numérico alvo a ser atingido (maior que zero)
 * @param idFarm      Identificador único da fazenda vinculada (opcional para produtor rural logado)
 */
@Schema(description = "Dados para cadastro de uma meta individual da fazenda")
public record IndividualGoalRequestDTO(

        @Schema(description = "Título da meta individual", example = "Reduzir consumo de energia")
        @NotBlank(message = "O título da meta não pode estar em branco")
        @Size(max = 50, message = "O título da meta deve ter no máximo 50 caracteres")
        String title,

        @Schema(description = "Descrição detalhada da meta", example = "Meta operacional para otimizar os ciclos de ventilação noturna")
        String description,

        @Schema(description = "Tipo ou métrica da meta (ex.: ENERGY_CONSUMPTION, WATER_CONSUMPTION, MORTALITY)", example = "ENERGY_CONSUMPTION")
        @NotBlank(message = "O tipo da meta não pode estar em branco")
        @Size(max = 50, message = "O tipo da meta deve ter no máximo 50 caracteres")
        String type,

        @Schema(description = "Status da meta", example = "IN_PROGRESS")
        @NotBlank(message = "O status da meta não pode estar em branco")
        @Size(max = 50, message = "O status da meta deve ter no máximo 50 caracteres")
        String status,

        @Schema(description = "Valor numérico alvo da meta", example = "420.5000")
        @JsonProperty("target_value")
        @JsonAlias("targetValue")
        @NotNull(message = "O valor alvo é obrigatório")
        @Positive(message = "O valor alvo deve ser maior que zero")
        @Digits(integer = 15, fraction = 4, message = "O valor alvo deve ter no máximo 15 dígitos inteiros e 4 casas decimais")
        BigDecimal targetValue,

        @Schema(description = "Identificador único da fazenda vinculada (obrigatório para ADM e funcionários; opcional para produtor rural)", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @Positive(message = "O ID da fazenda deve ser maior que zero")
        Long idFarm
) {
    /**
     * Construtor compacto para sanitização de strings.
     */
    public IndividualGoalRequestDTO {
        title = title != null ? title.trim() : null;
        description = description != null ? description.trim() : null;
        type = type != null ? type.trim() : null;
        status = status != null ? status.trim() : null;
    }
}
