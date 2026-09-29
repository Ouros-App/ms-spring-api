package com.ourosapp.springapi.dto.enterpriseplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.dto.enterprise.EnterpriseResponseDTO;
import com.ourosapp.springapi.dto.plan.PlanResponseDTO;
import com.ourosapp.springapi.entity.EnterprisePlan;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta detalhado de um plano contratado por uma Empresa Integradora.
 */
@Schema(description = "Dados detalhados do plano de assinatura contratado pela empresa integradora")
public record EnterprisePlanResponseDTO(

        @Schema(description = "Identificador único da contratação do plano", example = "1")
        Long id,

        @Schema(description = "Identificador da empresa integradora", example = "1")
        @JsonProperty("id_enterprise")
        Long idEnterprise,

        @Schema(description = "Identificador do plano de assinatura", example = "1")
        @JsonProperty("id_plan")
        Long idPlan,

        @Schema(description = "Dados detalhados do plano de assinatura (quando populado)")
        @JsonProperty("plan_details")
        PlanResponseDTO planDetails,

        @Schema(description = "Dados detalhados da empresa integradora (quando populado)")
        @JsonProperty("enterprise_details")
        EnterpriseResponseDTO enterpriseDetails
) {
    public static EnterprisePlanResponseDTO fromEntity(EnterprisePlan entity) {
        if (entity == null) {
            return null;
        }
        return new EnterprisePlanResponseDTO(
                entity.getId(),
                entity.getIdEnterprise(),
                entity.getIdPlan(),
                null,
                null
        );
    }

    public static EnterprisePlanResponseDTO fromEntityWithDetails(
            EnterprisePlan entity,
            PlanResponseDTO planDetails,
            EnterpriseResponseDTO enterpriseDetails
    ) {
        if (entity == null) {
            return null;
        }
        return new EnterprisePlanResponseDTO(
                entity.getId(),
                entity.getIdEnterprise(),
                entity.getIdPlan(),
                planDetails,
                enterpriseDetails
        );
    }
}
