package com.ourosapp.springapi.dto.stategoal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requisição para atualização parcial de uma Meta Estadual (PATCH /state-goals/{id}).
 *
 * @param status      Novo status da meta estadual (opcional, máx. 40 caracteres)
 * @param dateEnd     Nova data de término da vigência da meta (opcional)
 * @param targetValue Novo valor numérico alvo da meta (opcional, maior que zero)
 */
@Schema(description = "Dados para atualização parcial de uma meta estadual")
public record StateGoalUpdateDTO(

        @Schema(description = "Novo status da meta estadual", example = "ACHIEVED")
        @Size(max = 40, message = "O status da meta deve ter no máximo 40 caracteres")
        String status,

        @Schema(description = "Nova data de término da vigência da meta", example = "2026-11-30")
        @JsonProperty("date_end")
        @JsonAlias("dateEnd")
        LocalDate dateEnd,

        @Schema(description = "Novo valor numérico alvo da meta", example = "1.5500")
        @JsonProperty("target_value")
        @JsonAlias("targetValue")
        @Positive(message = "O valor alvo deve ser maior que zero")
        @Digits(integer = 15, fraction = 4, message = "O valor alvo deve ter no máximo 15 dígitos inteiros e 4 casas decimais")
        BigDecimal targetValue
) {
    /**
     * Construtor compacto para sanitização de strings.
     */
    public StateGoalUpdateDTO {
        status = status != null ? status.trim() : null;
    }

    /**
     * Verifica se ao menos um dos campos opcionais foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo preenchido, {@code false} caso contrário
     */
    public boolean hasUpdates() {
        return (status != null && !status.isBlank()) || dateEnd != null || targetValue != null;
    }
}
