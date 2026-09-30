package com.ourosapp.springapi.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta detalhado de uma transação de pagamento de plano.
 */
@Schema(description = "Dados detalhados da transação de pagamento de plano")
public record PaymentResponseDTO(

        @Schema(description = "Identificador único do pagamento", example = "1")
        Long id,

        @Schema(description = "Tipo/método de pagamento", example = "PIX")
        String type,

        @Schema(description = "Valor pago", example = "2990.00")
        BigDecimal value,

        @Schema(description = "Data e hora de realização do pagamento", example = "2026-09-29T10:30:00")
        @JsonProperty("date_creation")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dateCreation,

        @Schema(description = "Identificador da empresa integradora", example = "1")
        @JsonProperty("id_enterprise")
        Long idEnterprise,

        @Schema(description = "Identificador do plano da empresa vinculado", example = "1")
        @JsonProperty("id_enterprise_plan")
        Long idEnterprisePlan
) {
    public static PaymentResponseDTO fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getType(),
                payment.getValue(),
                payment.getDateCreation(),
                payment.getIdEnterprise(),
                payment.getIdEnterprisePlan()
        );
    }
}
