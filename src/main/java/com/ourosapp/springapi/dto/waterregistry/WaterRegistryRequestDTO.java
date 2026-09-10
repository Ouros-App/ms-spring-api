package com.ourosapp.springapi.dto.waterregistry;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requisição para cadastro de Registro de Medição de Água (POST /water-registries).
 *
 * @param registrationDate Data da leitura do hidrômetro
 * @param startHydrometer  Leitura inicial do hidrômetro em metros cúbicos (não negativa)
 * @param endHydrometer    Leitura final do hidrômetro em metros cúbicos (opcional no cadastro, não negativa)
 * @param idFarm           Identificador da fazenda vinculada (obrigatório para ADM e COMPANY_EMPLOYEE; inferido para FARM_OWNER caso omitido)
 */
@Schema(description = "Dados para cadastro de um novo registro de medição de água")
public record WaterRegistryRequestDTO(

        @Schema(description = "Data do registro da medição", example = "2026-09-10")
        @JsonProperty("registration_date")
        @JsonAlias("registrationDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @NotNull(message = "A data de registro é obrigatória")
        @PastOrPresent(message = "A data de registro não pode ser futura")
        LocalDate registrationDate,

        @Schema(description = "Leitura inicial do hidrômetro (m³)", example = "120.5000")
        @JsonProperty("start_hydrometer")
        @JsonAlias("startHydrometer")
        @NotNull(message = "A leitura inicial do hidrômetro é obrigatória")
        @Positive(message = "A leitura inicial do hidrômetro deve ser maior que zero")
        BigDecimal startHydrometer,

        @Schema(description = "Leitura final do hidrômetro (m³)", example = "135.8000")
        @JsonProperty("end_hydrometer")
        @JsonAlias("endHydrometer")
        @NotNull(message = "A leitura final do hidrômetro é obrigatória")
        @Positive(message = "A leitura final do hidrômetro deve ser maior que zero")
        BigDecimal endHydrometer,

        @Schema(description = "Identificador da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @Positive(message = "O ID da fazenda deve ser maior que zero")
        Long idFarm
) {

    /**
     * Validação cruzada para garantir que a leitura final do hidrômetro não seja menor que a inicial, quando informada.
     *
     * @return {@code true} se a leitura final for maior ou igual à leitura inicial ou se for nula
     */
    @Schema(hidden = true)
    @AssertTrue(message = "A leitura final do hidrômetro não pode ser menor que a leitura inicial")
    public boolean hasValidReadings() {
        if (startHydrometer == null || endHydrometer == null) {
            return true;
        }
        return endHydrometer.compareTo(startHydrometer) >= 0;
    }
}
