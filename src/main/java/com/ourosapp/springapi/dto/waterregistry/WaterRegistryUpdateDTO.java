package com.ourosapp.springapi.dto.waterregistry;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requisição para atualização parcial de Registro de Medição de Água (PATCH /water-registries/{id}).
 *
 * @param endHydrometer    Nova leitura final do hidrômetro (opcional, maior que zero)
 * @param startHydrometer  Nova leitura inicial do hidrômetro (opcional, maior que zero)
 * @param registrationDate Nova data de registro (opcional)
 */
@Schema(description = "Dados para atualização parcial do registro de medição de água")
public record WaterRegistryUpdateDTO(

        @Schema(description = "Nova leitura final do hidrômetro (m³)", example = "150.0000")
        @JsonProperty("end_hydrometer")
        @JsonAlias("endHydrometer")
        @Positive(message = "A leitura final do hidrômetro deve ser maior que zero")
        BigDecimal endHydrometer,

        @Schema(description = "Nova leitura inicial do hidrômetro (m³)", example = "120.0000")
        @JsonProperty("start_hydrometer")
        @JsonAlias("startHydrometer")
        @Positive(message = "A leitura inicial do hidrômetro deve ser maior que zero")
        BigDecimal startHydrometer,

        @Schema(description = "Nova data de registro", example = "2026-09-10")
        @JsonProperty("registration_date")
        @JsonAlias("registrationDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @PastOrPresent(message = "A data de registro não pode ser futura")
        LocalDate registrationDate
) {

    /**
     * Verifica se pelo menos um dos campos opcionais foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo não nulo
     */
    public boolean hasUpdates() {
        return endHydrometer != null || startHydrometer != null || registrationDate != null;
    }
}
