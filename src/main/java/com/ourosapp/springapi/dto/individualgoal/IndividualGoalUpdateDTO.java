package com.ourosapp.springapi.dto.individualgoal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de requisição para atualização parcial de uma Meta Individual (PATCH /individual-goals/{id}).
 *
 * @param title       Novo título da meta (opcional, máx. 50 caracteres)
 * @param description Nova descrição da meta (opcional)
 * @param status      Novo status da meta (opcional, máx. 50 caracteres)
 * @param targetValue Novo valor alvo da meta (opcional, maior que zero)
 */
@Schema(description = "Dados para atualização parcial de uma meta individual")
public record IndividualGoalUpdateDTO(

        @Schema(description = "Novo título da meta", example = "Reduzir consumo Galpão 1")
        @Size(max = 50, message = "O título da meta deve ter no máximo 50 caracteres")
        String title,

        @Schema(description = "Nova descrição da meta", example = "Meta atualizada com os novos parâmetros de ventilação")
        String description,

        @Schema(description = "Novo status da meta", example = "ACHIEVED")
        @Size(max = 50, message = "O status da meta deve ter no máximo 50 caracteres")
        String status,

        @Schema(description = "Novo valor numérico alvo da meta", example = "400.0000")
        @JsonProperty("target_value")
        @JsonAlias("targetValue")
        @Positive(message = "O valor alvo deve ser maior que zero")
        @Digits(integer = 15, fraction = 4, message = "O valor alvo deve ter no máximo 15 dígitos inteiros e 4 casas decimais")
        BigDecimal targetValue
) {
    /**
     * Construtor compacto para sanitização de strings.
     */
    public IndividualGoalUpdateDTO {
        title = title != null ? title.trim() : null;
        description = description != null ? description.trim() : null;
        status = status != null ? status.trim() : null;
    }

    /**
     * Verifica se ao menos um dos campos opcionais foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo preenchido, {@code false} caso contrário
     */
    public boolean hasUpdates() {
        return (title != null && !title.isBlank())
                || description != null
                || (status != null && !status.isBlank())
                || targetValue != null;
    }
}
