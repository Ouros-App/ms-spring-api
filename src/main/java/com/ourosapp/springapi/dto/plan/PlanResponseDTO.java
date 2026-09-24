package com.ourosapp.springapi.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.Plan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO de resposta contendo as informações completas de um Plano de Assinatura.
 */
@Schema(description = "Resposta com os dados detalhados do plano de assinatura")
public record PlanResponseDTO(

        @Schema(description = "Identificador único do plano", example = "1")
        Long id,

        @Schema(description = "Título do plano", example = "Plano Ouro Anual")
        String title,

        @Schema(description = "Duração do plano em dias", example = "365")
        @JsonProperty("duration_days")
        Integer durationDays,

        @Schema(description = "Descrição detalhada do plano", example = "Acesso completo a relatórios avançados e gestão de lotes.")
        String description,

        @Schema(description = "Preço da assinatura", example = "2990.00")
        BigDecimal price
) {
    public static PlanResponseDTO fromEntity(Plan plan) {
        if (plan == null) {
            return null;
        }
        return new PlanResponseDTO(
                plan.getId(),
                plan.getTitle(),
                plan.getDurationDays(),
                plan.getDescription(),
                plan.getPrice()
        );
    }
}
