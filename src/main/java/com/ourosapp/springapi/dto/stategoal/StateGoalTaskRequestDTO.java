package com.ourosapp.springapi.dto.stategoal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Meta descritiva acompanhada pelo status individual de cada granja, sem alvo numérico informado pelo usuário")
public record StateGoalTaskRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 50, message = "O título deve ter no máximo 50 caracteres")
        @Schema(description = "Título da meta", example = "Realizar manutenção preventiva") String title,
        @Schema(description = "Orientações para os participantes") String description,
        @JsonProperty("date_creation") @JsonAlias("dateCreation")
        @NotNull(message = "A data de início é obrigatória")
        @Schema(description = "Data de início", example = "2026-11-01") LocalDate dateCreation,
        @JsonProperty("date_end") @JsonAlias("dateEnd")
        @Schema(description = "Prazo da meta", example = "2026-12-01") LocalDate dateEnd
) {
    public StateGoalTaskRequestDTO {
        title = title == null ? null : title.trim();
        description = description == null ? null : description.trim();
    }

    @AssertTrue(message = "O prazo não pode ser anterior ao início")
    @Schema(hidden = true)
    public boolean isDateRangeValid() {
        return dateCreation == null || dateEnd == null || !dateEnd.isBefore(dateCreation);
    }
}
