package com.ourosapp.springapi.dto.chickenleft;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * DTO de requisição para atualização parcial de Registro de Saída de Aves (PATCH /chicken-left/{id}).
 *
 * @param chickensCount Nova quantidade de aves de saída (opcional, maior que zero)
 * @param exitDate      Nova data de saída das aves (opcional, não futura)
 */
@Schema(description = "Dados para atualização parcial do registro de saída de aves")
public record ChickenLeftUpdateDTO(

        @Schema(description = "Nova quantidade de aves de saída", example = "600")
        @JsonProperty("chickens_count")
        @JsonAlias("chickensCount")
        @Positive(message = "A quantidade de aves de saída deve ser maior que zero")
        Integer chickensCount,

        @Schema(description = "Nova data da saída das aves", example = "2026-09-21")
        @JsonProperty("exit_date")
        @JsonAlias("exitDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @PastOrPresent(message = "A data de saída não pode ser uma data futura")
        LocalDate exitDate
) {

    /**
     * Verifica se pelo menos um dos campos opcionais foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo não nulo
     */
    public boolean hasUpdates() {
        return chickensCount != null || exitDate != null;
    }
}
