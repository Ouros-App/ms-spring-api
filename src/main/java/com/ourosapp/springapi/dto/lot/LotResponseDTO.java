package com.ourosapp.springapi.dto.lot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Lot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO de resposta contendo as informações completas de um Lote de Aves.
 *
 * @param id                Identificador único do lote
 * @param receivedChickens  Quantidade de aves recebidas/alojadas
 * @param deliveredChickens Quantidade de aves entregues ao término do ciclo
 * @param deliveryDate      Data de entrega/abate
 * @param losts             Total de perdas / mortalidade
 * @param cost              Custo operacional total acumulado
 * @param idEnterprise      Identificador da empresa integradora vinculada
 * @param idFarm            Identificador da fazenda vinculada
 */
@Schema(description = "Resposta contendo os dados do Lote de Aves")
public record LotResponseDTO(

        @Schema(description = "Identificador único do lote", example = "1")
        Long id,

        @Schema(description = "Quantidade de aves recebidas no início do lote", example = "50000")
        @JsonProperty("received_chickens")
        Integer receivedChickens,

        @Schema(description = "Quantidade de aves entregues ao final do ciclo", example = "48500")
        @JsonProperty("delivered_chickens")
        Integer deliveredChickens,

        @Schema(description = "Data de entrega para abate", example = "2026-10-15")
        @JsonProperty("delivery_date")
        LocalDate deliveryDate,

        @Schema(description = "Total de perdas ou mortalidade", example = "1500")
        Integer losts,

        @Schema(description = "Custo operacional total acumulado", example = "12500.50")
        Double cost,

        @Schema(description = "Identificador da empresa integradora vinculada", example = "1")
        @JsonProperty("id_enterprise")
        Long idEnterprise,

        @Schema(description = "Identificador da fazenda onde o lote foi alojado", example = "1")
        @JsonProperty("id_farm")
        Long idFarm
) {

    /**
     * Converte uma entidade {@link Lot} em {@link LotResponseDTO}.
     *
     * @param lot entidade a ser convertida (não deve ser nula)
     * @return DTO correspondente
     * @throws NullPointerException se lot for nulo
     */
    public static LotResponseDTO fromEntity(Lot lot) {
        Objects.requireNonNull(lot, "Lot não pode ser nulo");
        return new LotResponseDTO(
                lot.getId(),
                lot.getReceivedChickens(),
                lot.getDeliveredChickens(),
                lot.getDeliveryDate(),
                lot.getLosts(),
                lot.getCost(),
                lot.getIdEnterprise(),
                lot.getIdFarm()
        );
    }
}
