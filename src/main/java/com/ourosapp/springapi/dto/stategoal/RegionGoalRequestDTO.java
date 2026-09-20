package com.ourosapp.springapi.dto.stategoal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para vincular uma nova região a uma meta estadual.
 *
 * @param region Nome da região (ex.: "Sudeste", "Sul", "Centro-Oeste")
 */
@Schema(description = "Dados para vincular uma nova região a uma meta estadual")
public record RegionGoalRequestDTO(

        @Schema(description = "Nome da região", example = "Sudeste")
        @NotBlank(message = "A região não pode estar em branco")
        @Size(max = 50, message = "A região deve ter no máximo 50 caracteres")
        String region
) {
    public RegionGoalRequestDTO {
        region = region != null ? region.trim() : null;
    }
}
