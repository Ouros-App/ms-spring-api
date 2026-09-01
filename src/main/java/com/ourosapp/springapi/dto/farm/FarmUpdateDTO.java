package com.ourosapp.springapi.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO de requisição para atualização parcial de Fazenda (PATCH /farms/{id}).
 * Todos os campos são opcionais, permitindo atualizar apenas as informações fornecidas.
 *
 * @param name            Novo nome da fazenda (opcional)
 * @param areaProperty    Nova área da propriedade (opcional, maior que zero)
 * @param region          Nova região da fazenda (opcional)
 * @param poultryCapacity Nova capacidade de alojamento de aves (opcional, não negativa)
 * @param place           Novo local ou sítio/granja (opcional)
 */
@Schema(description = "Dados para atualização parcial da fazenda")
public record FarmUpdateDTO(

        @Schema(description = "Novo nome da fazenda", example = "Fazenda Santa Maria Renomeada")
        @Size(max = 100, message = "O nome da fazenda deve ter no máximo 100 caracteres")
        String name,

        @Schema(description = "Nova área da propriedade", example = "200.00")
        @JsonProperty("area_property")
        @JsonAlias("areaProperty")
        @Positive(message = "A área da propriedade deve ser maior que zero")
        BigDecimal areaProperty,

        @Schema(description = "Nova região da fazenda", example = "Centro-Oeste")
        @Size(max = 50, message = "A região deve ter no máximo 50 caracteres")
        String region,

        @Schema(description = "Nova capacidade de alojamento de aves", example = "60000")
        @JsonProperty("poultry_capacity")
        @JsonAlias("poultryCapacity")
        @Min(value = 0, message = "A capacidade de aves não pode ser negativa")
        Integer poultryCapacity,

        @Schema(description = "Novo local ou denominação do sítio/granja", example = "Gleba 5 - Setor Norte")
        @Size(max = 50, message = "O local deve ter no máximo 50 caracteres")
        String place
) {

    /**
     * Construtor compacto para sanitização de espaços em branco dos campos de texto informados.
     */
    public FarmUpdateDTO {
        name = name != null ? name.trim() : null;
        region = region != null ? region.trim() : null;
        place = place != null ? place.trim() : null;
    }

    /**
     * Verifica se pelo menos um dos campos opcionais foi informado para atualização.
     *
     * @return {@code true} se houver pelo menos um campo não nulo e não vazio
     */
    public boolean hasUpdates() {
        return (name != null && !name.isBlank())
                || areaProperty != null
                || (region != null && !region.isBlank())
                || poultryCapacity != null
                || (place != null && !place.isBlank());
    }
}
