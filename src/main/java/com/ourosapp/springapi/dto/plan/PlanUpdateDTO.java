package com.ourosapp.springapi.dto.plan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de requisição para atualização parcial ou total de um Plano de Assinatura (PATCH/PUT /plans/{id}).
 */
@Schema(description = "Dados para atualização de um plano de assinatura existente")
public record PlanUpdateDTO(

        @Schema(description = "Novo título do plano", example = "Plano Ouro Semestral")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
        String title,

        @Schema(description = "Nova duração do plano em dias", example = "180")
        @JsonProperty("duration_days")
        @JsonAlias("durationDays")
        @Positive(message = "A duração em dias deve ser maior que zero")
        @Min(value = 1, message = "A duração mínima é de 1 dia")
        Integer durationDays,

        @Schema(description = "Nova descrição do plano", example = "Acesso a relatórios e suporte comercial.")
        String description,

        @Schema(description = "Novo preço do plano", example = "1590.00")
        @Positive(message = "O preço do plano deve ser maior que zero")
        @DecimalMin(value = "0.01", message = "O preço mínimo do plano é R$ 0.01")
        BigDecimal price
) {
    public PlanUpdateDTO {
        title = title != null ? title.trim() : null;
        description = description != null ? description.trim() : null;
    }

    public boolean hasUpdates() {
        return title != null
                || durationDays != null
                || description != null
                || price != null;
    }
}
