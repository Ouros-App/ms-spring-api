package com.ourosapp.springapi.dto.waterregistry;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.WaterRegistry;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO de resposta contendo os dados de um Registro de Medição de Água.
 *
 * @param id               Identificador único do registro
 * @param registrationDate Data do registro da medição
 * @param startHydrometer  Leitura inicial do hidrômetro
 * @param endHydrometer    Leitura final do hidrômetro
 * @param idFarm           Identificador da fazenda vinculada
 */
@Schema(description = "Resposta contendo os dados do Registro de Medição de Água")
public record WaterRegistryResponseDTO(

        @Schema(description = "Identificador único do registro", example = "1")
        Long id,

        @Schema(description = "Data do registro da medição", example = "2026-09-10")
        @JsonProperty("registration_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate registrationDate,

        @Schema(description = "Leitura inicial do hidrômetro (m³)", example = "120.5000")
        @JsonProperty("start_hydrometer")
        BigDecimal startHydrometer,

        @Schema(description = "Leitura final do hidrômetro (m³)", example = "135.8000")
        @JsonProperty("end_hydrometer")
        BigDecimal endHydrometer,

        @Schema(description = "Identificador da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        Long idFarm
) {

    /**
     * Converte uma entidade {@link WaterRegistry} em {@link WaterRegistryResponseDTO}.
     *
     * @param entity entidade a ser convertida (não deve ser nula)
     * @return DTO correspondente
     * @throws NullPointerException se entity for nula
     */
    public static WaterRegistryResponseDTO fromEntity(WaterRegistry entity) {
        Objects.requireNonNull(entity, "WaterRegistry não pode ser nulo");
        return new WaterRegistryResponseDTO(
                entity.getId(),
                entity.getRegistrationDate(),
                entity.getStartHydrometer(),
                entity.getEndHydrometer(),
                entity.getIdFarm()
        );
    }
}
