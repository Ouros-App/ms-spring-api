package com.ourosapp.springapi.dto.lot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados do lote e leitura inicial opcional do hidrômetro")
public record LotAggregatedRequestDTO(
    @Schema(description = "Quantidade de aves recebidas", example = "50000") @JsonProperty("received_chickens") @JsonAlias("receivedChickens") @NotNull(message = "A quantidade de aves recebidas é obrigatória") @Min(value = 0, message = "A quantidade de aves recebidas não pode ser negativa") Integer receivedChickens,
    @Schema(description = "Quantidade de aves entregues", example = "0") @JsonProperty("delivered_chickens") @JsonAlias("deliveredChickens") @Min(value = 0, message = "A quantidade de aves entregues não pode ser negativa") Integer deliveredChickens,
    @Schema(description = "Data de entrega", example = "2026-10-15") @JsonProperty("delivery_date") @JsonAlias("deliveryDate") LocalDate deliveryDate,
    @Schema(description = "Custo do lote", example = "100.0") @JsonProperty("cost") @DecimalMin(value = "0.0", message = "O custo não pode ser negativo") Double cost,
    @Schema(description = "Identificador da fazenda", example = "1") @JsonProperty("id_farm") @JsonAlias("idFarm") @NotNull(message = "O ID da fazenda é obrigatório") @Positive(message = "O ID da fazenda deve ser maior que zero") Long idFarm,
    @Schema(description = "Identificador da empresa", example = "1") @JsonProperty("id_enterprise") @JsonAlias("idEnterprise") @Positive(message = "O ID da empresa deve ser maior que zero") Long idEnterprise,
    @Schema(description = "Leitura inicial do hidrômetro em m³; zero ou ausência não cria medição", example = "120.5000") @JsonProperty("hydrometer_reading") @JsonAlias("hydrometerReading") @DecimalMin(value = "0.0", message = "A leitura não pode ser negativa") @Digits(integer = 15, fraction = 4, message = "A leitura deve ter no máximo 15 dígitos inteiros e 4 casas decimais") BigDecimal hydrometerReading
) {
    @Schema(hidden = true)
    @AssertTrue(message = "A quantidade de aves entregues não pode ser superior à quantidade de aves recebidas")
    public boolean isDeliveredChickensValid() {
        return deliveredChickens == null || receivedChickens == null || deliveredChickens <= receivedChickens;
    }
}
