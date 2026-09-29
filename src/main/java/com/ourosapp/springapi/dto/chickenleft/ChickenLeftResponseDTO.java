package com.ourosapp.springapi.dto.chickenleft;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.ChickenLeft;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO de resposta contendo os dados de um Registro de Saída e Baixa de Aves.
 *
 * @param id            Identificador único do registro
 * @param chickensCount Quantidade de aves de saída
 * @param exitDate      Data de saída das aves
 * @param idFarm        Identificador da fazenda vinculada
 */
@Schema(description = "Resposta contendo os dados do Registro de Saída de Aves")
public record ChickenLeftResponseDTO(

        @Schema(description = "Identificador único do registro", example = "1")
        Long id,

        @Schema(description = "Quantidade de aves de saída", example = "500")
        @JsonProperty("chickens_count")
        Integer chickensCount,

        @Schema(description = "Data da saída das aves", example = "2026-09-20")
        @JsonProperty("exit_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate exitDate,

        @Schema(description = "Identificador da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        Long idFarm
) {

    /**
     * Converte uma entidade {@link ChickenLeft} em {@link ChickenLeftResponseDTO}.
     *
     * @param entity entidade a ser convertida (não deve ser nula)
     * @return DTO correspondente
     * @throws NullPointerException se entity for nula
     */
    public static ChickenLeftResponseDTO fromEntity(ChickenLeft entity) {
        Objects.requireNonNull(entity, "ChickenLeft não pode ser nulo");
        return new ChickenLeftResponseDTO(
                entity.getId(),
                entity.getChickensCount(),
                entity.getExitDate(),
                entity.getIdFarm()
        );
    }
}
