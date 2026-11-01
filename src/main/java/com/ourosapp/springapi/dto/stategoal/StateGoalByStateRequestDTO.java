package com.ourosapp.springapi.dto.stategoal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;

@Schema(description = "Meta compartilhada pelas granjas brasileiras da UF na empresa selecionada")
public record StateGoalByStateRequestDTO(
        @Schema(description = "UF brasileira", example = "SP")
        @NotBlank(message = "A UF é obrigatória")
        @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO", message = "Informe uma UF brasileira válida")
        String state,
        @Schema(description = "Empresa obrigatória para ADM; para funcionário, inferida da identidade", example = "1")
        @JsonProperty("id_enterprise") @JsonAlias("idEnterprise")
        @Positive(message = "O ID da empresa deve ser positivo") Long idEnterprise,
        @Schema(description = "Dados da meta; não informe id_farm nem region")
        @Valid @NotNull(message = "Os dados da meta são obrigatórios") StateGoalRequestDTO goal
) {
    public StateGoalByStateRequestDTO {
        state = state == null ? null : state.trim().toUpperCase(Locale.ROOT);
    }
}
