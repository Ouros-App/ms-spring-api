package com.ourosapp.springapi.dto.payment;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de requisição para registro de uma transação de pagamento de plano.
 */
@Schema(description = "Dados para registro de pagamento de plano por empresa integradora")
public record PaymentRequestDTO(

        @Schema(description = "Tipo/método de pagamento realizado (ex.: PIX, CREDIT_CARD, BOLETO, TRANSFER)", example = "PIX")
        @NotBlank(message = "O tipo de pagamento não pode estar em branco")
        @Size(max = 50, message = "O tipo de pagamento deve ter no máximo 50 caracteres")
        String type,

        @Schema(description = "Valor financeiro liquidado", example = "2990.00")
        @NotNull(message = "O valor do pagamento é obrigatório")
        @Positive(message = "O valor do pagamento deve ser maior que zero")
        @DecimalMin(value = "0.01", message = "O valor mínimo de pagamento é R$ 0.01")
        BigDecimal value,

        @Schema(description = "Identificador da empresa integradora (obrigatório para ADM, inferido para COMPANY_EMPLOYEE)", example = "1")
        @JsonProperty("id_enterprise")
        @JsonAlias("idEnterprise")
        @Positive(message = "O ID da empresa integradora deve ser maior que zero")
        Long idEnterprise,

        @Schema(description = "Identificador do plano contratado vinculado ao pagamento", example = "1")
        @JsonProperty("id_enterprise_plan")
        @JsonAlias("idEnterprisePlan")
        @NotNull(message = "O ID do plano da empresa é obrigatório")
        @Positive(message = "O ID do plano da empresa deve ser maior que zero")
        Long idEnterprisePlan
) {
    public PaymentRequestDTO {
        type = type != null ? type.trim().toUpperCase() : null;
    }
}
