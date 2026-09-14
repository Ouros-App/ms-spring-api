package com.ourosapp.springapi.dto.lot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requisição para cadastro e início de um novo Lote de Aves (POST /lots).
 *
 * @param receivedChickens  Quantidade de aves recebidas/alojadas
 * @param deliveredChickens Quantidade de aves entregues (opcional na inicialização)
 * @param dateBirth         Data de nascimento ou início do alojamento
 * @param deliveryDate      Data prevista ou efetiva de entrega (opcional na inicialização)
 * @param gain              Ganho de peso acumulado ou financeiro (opcional, padrão 0.0)
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

        @Schema(description = "Data de nascimento ou alojamento das aves", example = "2026-09-01")
        @JsonProperty("date_birth")
        @JsonAlias("dateBirth")
        @NotNull(message = "A data de nascimento/alojamento é obrigatória")
        LocalDate dateBirth,

        @Schema(description = "Data de entrega para abate ou finalização do ciclo (opcional no início; quando omitida, é inicializada com o mesmo valor de date_birth)", example = "2026-10-15")
        @JsonProperty("delivery_date")
        @JsonAlias("deliveryDate")
        LocalDate deliveryDate,

        @Schema(description = "Ganho de peso acumulado ou financeiro", example = "2.8500")
        @JsonProperty("gain")
        @DecimalMin(value = "0.0", message = "O ganho não pode ser negativo")
        BigDecimal gain,

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

    /**
     * Validação cruzada para garantir que a data de entrega não seja anterior à data de nascimento/alojamento.
     *
     * @return {@code true} se deliveryDate for nula ou posterior/igual a dateBirth
     */
    @Schema(hidden = true)
    @AssertTrue(message = "A data de entrega deve ser posterior ou igual à data de nascimento/alojamento")
    public boolean isDeliveryDateValid() {
        if (deliveryDate == null || dateBirth == null) {
            return true;
        }
        return !deliveryDate.isBefore(dateBirth);
    }
}
