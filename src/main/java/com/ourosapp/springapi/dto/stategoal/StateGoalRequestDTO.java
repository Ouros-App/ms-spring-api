package com.ourosapp.springapi.dto.stategoal;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requisição para cadastro de Meta Estadual (POST /state-goals).
 *
 * @param title        Título descritivo da meta estadual (máx. 50 caracteres)
 * @param description  Detalhamento ou observações da meta
 * @param type         Tipo ou categoria da meta (máx. 50 caracteres)
 * @param status       Status da meta (máx. 40 caracteres)
 * @param targetValue  Valor numérico alvo a ser atingido (maior que zero)
 * @param dateCreation Data de início/criação da vigência da meta estadual
 * @param dateEnd      Data de término/conclusão da vigência da meta estadual
 * @param idFarm       Identificador único da fazenda vinculada (opcional para produtor rural logado)
 * @param region       Região de abrangência da meta (opcional, inferida da fazenda vinculada)
 */
@Schema(description = "Dados para cadastro de uma meta estadual")
public record StateGoalRequestDTO(

        @Schema(description = "Título da meta estadual", example = "Meta Regional Conversão SP")
        @NotBlank(message = "O título da meta não pode estar em branco")
        @Size(max = 50, message = "O título da meta deve ter no máximo 50 caracteres")
        String title,

        @Schema(description = "Descrição detalhada da meta", example = "Meta estabelecida para o estado com base na média regional de produtividade")
        String description,

        @Schema(description = "Tipo ou métrica da meta", example = "FEED_CONVERSION")
        @NotBlank(message = "O tipo da meta não pode estar em branco")
        @Size(max = 50, message = "O tipo da meta deve ter no máximo 50 caracteres")
        String type,

        @Schema(description = "Status da meta", example = "IN_PROGRESS")
        @NotBlank(message = "O status da meta não pode estar em branco")
        @Size(max = 40, message = "O status da meta deve ter no máximo 40 caracteres")
        String status,

        @Schema(description = "Valor numérico alvo da meta", example = "1.6500")
        @JsonProperty("target_value")
        @JsonAlias("targetValue")
        @NotNull(message = "O valor alvo é obrigatório")
        @Positive(message = "O valor alvo deve ser maior que zero")
        @Digits(integer = 15, fraction = 4, message = "O valor alvo deve ter no máximo 15 dígitos inteiros e 4 casas decimais")
        BigDecimal targetValue,

        @Schema(description = "Data de início da vigência da meta", example = "2026-01-01")
        @JsonProperty("date_creation")
        @JsonAlias("dateCreation")
        @NotNull(message = "A data de criação/início é obrigatória")
        LocalDate dateCreation,

        @Schema(description = "Data de término da vigência da meta", example = "2026-12-31")
        @JsonProperty("date_end")
        @JsonAlias("dateEnd")
        @NotNull(message = "A data de término é obrigatória")
        LocalDate dateEnd,

        @Schema(description = "Identificador único da fazenda vinculada (obrigatório para ADM e funcionários; opcional para produtor rural)", example = "1")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        @Positive(message = "O ID da fazenda deve ser maior que zero")
        Long idFarm,

        @Schema(description = "Região da meta estadual (opcional, inferida da fazenda se não informada)", example = "Sudeste")
        @Size(max = 50, message = "A região deve ter no máximo 50 caracteres")
        String region
) {
    /**
     * Construtor compacto para sanitização de strings.
     */
    public StateGoalRequestDTO {
        title = title != null ? title.trim() : null;
        description = description != null ? description.trim() : null;
        type = type != null ? type.trim() : null;
        status = status != null ? status.trim() : null;
        region = region != null ? region.trim() : null;
    }

    /**
     * Validação cruzada garantindo que a data de término seja posterior ou igual à data de criação.
     *
     * @return {@code true} se o intervalo de datas for válido
     */
    @Schema(hidden = true)
    @AssertTrue(message = "A data de término deve ser posterior ou igual à data de criação")
    public boolean isDateRangeValid() {
        if (dateCreation == null || dateEnd == null) {
            return true;
        }
        return !dateEnd.isBefore(dateCreation);
    }
}
