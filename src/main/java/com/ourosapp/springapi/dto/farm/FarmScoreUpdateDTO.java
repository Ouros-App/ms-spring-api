package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO para atualização pontual ou recálculo manual de pontuação de ranking de uma granja.
 *
 * @param score Pontuação a ser atribuída à fazenda no ranking
 */
@Schema(description = "Requisição para atualização de score de ranking da Granja")
public record FarmScoreUpdateDTO(

        @Schema(description = "Pontuação a ser atribuída", example = "95.5")
        @NotNull(message = "A pontuação (score) é obrigatória")
        @PositiveOrZero(message = "A pontuação deve ser maior ou igual a zero")
        @JsonProperty("score")
        @JsonAlias("score")
        Double score
) {}
