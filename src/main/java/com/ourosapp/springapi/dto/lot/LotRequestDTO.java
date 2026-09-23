package com.ourosapp.springapi.dto.lot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * DTO de requisição para cadastro e início de um novo Lote de Aves (POST /lots).
 *
 * @param receivedChickens  Quantidade de aves recebidas/alojadas
 * @param deliveredChickens Quantidade de aves entregues (opcional na inicialização)
 * @param deliveryDate      Data de entrega para abate ou finalização do ciclo
 * @param losts             Total de perdas / mortalidade (opcional, padrão 0)
 * @param cost              Custo operacional total (opcional, padrão 0.0)
 * @param idEnterprise      Identificador da empresa integradora vinculada
 * @param idFarm            Identificador da fazenda onde o lote será alojado
 */
@Schema(description = "Dados para cadastro e início de um novo lote de aves")
public record LotRequestDTO(

        @Schema(description = "Quantidade de aves recebidas e alojadas no início do lote", example = "50000")
        @JsonProperty("received_chickens")
        @JsonAlias("receivedChickens")
        @NotNull(message = "A quantidade de aves recebidas é obrigatória")
        @Min(value = 0, message = "A quantidade de aves recebidas não pode ser negativa")
        Integer receivedChickens,

        @Schema(description = "Quantidade de aves entregues ao final do ciclo (opcional no início)", example = "48500")
        @JsonProperty("delivered_chickens")
        @JsonAlias("deliveredChickens")
        @Min(value = 0, message = "A quantidade de aves entregues não pode ser negativa")
        Integer deliveredChickens,

        @Schema(description = "Data de entrega para abate ou finalização do ciclo", example = "2026-10-15")
        @JsonProperty("delivery_date")
        @JsonAlias("deliveryDate")
        @NotNull(message = "A data de entrega é obrigatória")
        LocalDate deliveryDate,

        @Schema(description = "Total de perdas ou mortalidade acumulada", example = "1500")
        @JsonProperty("losts")
        @Min(value = 0, message = "A quantidade de perdas não pode ser negativa")
        Integer losts,

        @Schema(description = "Custo operacional total acumulado do lote", example = "12500.50")
        @JsonProperty("cost")
        @DecimalMin(value = "0.0", message = "O custo não pode ser negativo")
        Double cost,

        @Schema(description = "Identificador da empresa integradora vinculada (opcional, inferido automaticamente a partir do funcionário logado ou da fazenda)", example = "1")
        @JsonProperty("id_enterprise")
        @JsonAlias("idEnterprise")
        @Positive(message = "O ID da empresa integradora deve ser maior que zero")
        Long idEnterprise,

        @Schema(description = "Identificador da fazenda onde o lote está alojado", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @NotNull(message = "O ID da fazenda é obrigatório")
        @Positive(message = "O ID da fazenda deve ser maior que zero")
        Long idFarm
) {

    /**
     * Validação cruzada para garantir que as aves entregues não excedam as aves recebidas.
     *
     * @return {@code true} se deliveredChickens for nulo ou menor/igual a receivedChickens
     */
    @Schema(hidden = true)
    @AssertTrue(message = "A quantidade de aves entregues não pode ser superior à quantidade de aves recebidas")
    public boolean isDeliveredChickensValid() {
        if (deliveredChickens == null || receivedChickens == null) {
            return true;
        }
        return deliveredChickens <= receivedChickens;
    }
}
