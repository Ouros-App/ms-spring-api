package com.ourosapp.springapi.dto.plan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO de requisição para cadastro de um novo Plano de Assinatura (POST /plans).
 */
@Schema(description = "Dados para cadastro de um novo plano de assinatura")
public record PlanRequestDTO(

        @Schema(description = "Título identificador do plano", example = "Plano Ouro Anual")
        @NotBlank(message = "O título do plano não pode estar em branco")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
        String title,

        @Schema(description = "Duração do plano em dias", example = "365")
        @JsonProperty("duration_days")
        @JsonAlias("durationDays")
        @NotNull(message = "A duração em dias é obrigatória")
        @Positive(message = "A duração em dias deve ser maior que zero")
        @Min(value = 1, message = "A duração mínima do plano é de 1 dia")
        Integer durationDays,

        @Schema(description = "Descrição detalhada dos benefícios e limites do plano", example = "Acesso completo a relatórios avançados, gestão de lotes e suporte prioritário.")
        @NotBlank(message = "A descrição do plano não pode estar em branco")
        String description,

        @Schema(description = "Valor monetário da assinatura do plano", example = "2990.00")
        @NotNull(message = "O preço do plano é obrigatório")
        @Positive(message = "O preço do plano deve ser maior que zero")
        @DecimalMin(value = "0.01", message = "O preço mínimo do plano é R$ 0.01")
        BigDecimal price
) {
    public PlanRequestDTO {
        title = title != null ? title.trim() : null;
        description = description != null ? description.trim() : null;
    }
}
