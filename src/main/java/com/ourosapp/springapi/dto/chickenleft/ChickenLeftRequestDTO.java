package com.ourosapp.springapi.dto.chickenleft;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * DTO de requisição para cadastro de Saída e Baixa de Aves (POST /chicken-left).
 *
 * @param chickensCount Quantidade de aves que saíram da fazenda (maior que zero)
 * @param exitDate      Data de saída das aves (não futura)
 * @param idFarm        Identificador da fazenda vinculada (obrigatório para ADM e COMPANY_EMPLOYEE; inferido para FARM_OWNER caso omitido)
 */
@Schema(description = "Dados para cadastro de um novo registro de saída de aves")
public record ChickenLeftRequestDTO(

        @Schema(description = "Quantidade de aves de saída", example = "500")
        @JsonProperty("chickens_count")
        @JsonAlias("chickensCount")
        @NotNull(message = "A quantidade de aves de saída é obrigatória")
        @Positive(message = "A quantidade de aves de saída deve ser maior que zero")
        Integer chickensCount,

        @Schema(description = "Data da saída das aves", example = "2026-09-20")
        @JsonProperty("exit_date")
        @JsonAlias("exitDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @NotNull(message = "A data de saída é obrigatória")
        @PastOrPresent(message = "A data de saída não pode ser uma data futura")
        LocalDate exitDate,

        @Schema(description = "Identificador único da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @Positive(message = "O ID da fazenda deve ser maior que zero")
        Long idFarm
) {
}
