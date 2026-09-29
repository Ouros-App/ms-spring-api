package com.ourosapp.springapi.dto.enterpriseplan;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO de requisição para contratação / adesão de um Plano por uma Empresa Integradora.
 */
@Schema(description = "Dados para contratação de um plano de assinatura por uma empresa integradora")
public record EnterprisePlanRequestDTO(

        @Schema(description = "Identificador da empresa integradora (obrigatório para ADM, inferido para COMPANY_EMPLOYEE)", example = "1")
        @JsonProperty("id_enterprise")
        @JsonAlias("idEnterprise")
        @Positive(message = "O ID da empresa integradora deve ser maior que zero")
        Long idEnterprise,

        @Schema(description = "Identificador do plano de assinatura contratado", example = "1")
        @JsonProperty("id_plan")
        @JsonAlias("idPlan")
        @NotNull(message = "O ID do plano de assinatura é obrigatório")
        @Positive(message = "O ID do plano de assinatura deve ser maior que zero")
        Long idPlan
) {}
