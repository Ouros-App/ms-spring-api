package com.ourosapp.springapi.dto.lot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requisição para atualização parcial de um Lote de Aves (PATCH /lots/{id}).
 * Utilizado principalmente no encerramento ou ajuste do ciclo produtivo.
 * Todos os campos são opcionais.
 *
 * @param receivedChickens  Nova quantidade de aves recebidas (opcional)
 * @param deliveredChickens Quantidade final de aves entregues (opcional)
 * @param dateBirth         Data de nascimento ou início do alojamento (opcional)
 * @param deliveryDate      Data final de entrega/abate (opcional)
 * @param gain              Ganho acumulado de peso ou financeiro (opcional)
 * @param losts             Total de perdas / mortalidade registradas (opcional)
 * @param cost              Custo total apurado do lote (opcional)
 */
@Schema(description = "Dados para atualização parcial ou fechamento do ciclo do lote")
public record LotUpdateDTO(

        @Schema(description = "Quantidade de aves recebidas", example = "50000")
        @JsonProperty("received_chickens")
        @JsonAlias("receivedChickens")
        @Min(value = 0, message = "A quantidade de aves recebidas não pode ser negativa")
        Integer receivedChickens,

        @Schema(description = "Quantidade final de aves entregues", example = "48500")
        @JsonProperty("delivered_chickens")
        @JsonAlias("deliveredChickens")
        @Min(value = 0, message = "A quantidade de aves entregues não pode ser negativa")
        Integer deliveredChickens,

        @Schema(description = "Data de nascimento ou alojamento das aves", example = "2026-09-01")
        @JsonProperty("date_birth")
        @JsonAlias("dateBirth")
        LocalDate dateBirth,

        @Schema(description = "Data final de entrega para abate", example = "2026-10-15")
        @JsonProperty("delivery_date")
        @JsonAlias("deliveryDate")
        LocalDate deliveryDate,

        @Schema(description = "Ganho acumulado de peso ou financeiro", example = "2.8500")
        @JsonProperty("gain")
        @DecimalMin(value = "0.0", message = "O ganho não pode ser negativo")
        BigDecimal gain,

        @Schema(description = "Total de perdas acumuladas", example = "1500")
        @JsonProperty("losts")
        @Min(value = 0, message = "A quantidade de perdas não pode ser negativa")
        Integer losts,

        @Schema(description = "Custo operacional total apurado", example = "12500.50")
        @JsonProperty("cost")
        @DecimalMin(value = "0.0", message = "O custo não pode ser negativo")
        Double cost
) {

    /**
     * Validação cruzada caso ambos os campos de quantidade sejam fornecidos simultaneamente.
     *
     * @return {@code true} se deliveredChickens for menor ou igual a receivedChickens
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
     * Validação cruzada caso ambas as datas sejam fornecidas simultaneamente.
     *
     * @return {@code true} se deliveryDate for igual ou posterior a dateBirth
     */
    @Schema(hidden = true)
    @AssertTrue(message = "A data de entrega deve ser posterior ou igual à data de nascimento/alojamento")
    public boolean isDeliveryDateValid() {
        if (deliveryDate == null || dateBirth == null) {
            return true;
        }
        return !deliveryDate.isBefore(dateBirth);
    }

    /**
     * Verifica se pelo menos um dos campos foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo não nulo
     */
    public boolean hasUpdates() {
        return receivedChickens != null
                || deliveredChickens != null
                || dateBirth != null
                || deliveryDate != null
                || gain != null
                || losts != null
                || cost != null;
    }
}
