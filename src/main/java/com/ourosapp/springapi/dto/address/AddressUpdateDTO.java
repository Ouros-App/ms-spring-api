package com.ourosapp.springapi.dto.address;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para atualização parcial de endereços (PATCH).
 * Todos os campos são opcionais.
 */
@Schema(description = "Dados para atualização parcial do endereço")
public record AddressUpdateDTO(

        @Schema(description = "Novo CEP do endereço", example = "12345678")
        @JsonProperty("zip_code")
        @JsonAlias("zipCode")
        @Size(max = 50, message = "O CEP deve ter no máximo 50 caracteres")
        String zipCode,

        @Schema(description = "Novo Estado do endereço (UF com 2 caracteres)", example = "SP")
        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "O estado deve conter exatamente 2 letras")
        String state,

        @Schema(description = "Nova Cidade do endereço", example = "Campinas")
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String city,

        @Schema(description = "Novo Número do endereço", example = "555")
        @Size(max = 50, message = "O número deve ter no máximo 50 caracteres")
        String number,

        @Schema(description = "Novo País do endereço (código com 2 caracteres)", example = "BR")
        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "O país deve conter código ISO de 2 letras")
        String country
) {
    /**
     * Construtor compacto para normalização e sanitização.
     */
    public AddressUpdateDTO {
        zipCode = zipCode != null ? zipCode.trim() : null;
        state = state != null ? state.trim().toUpperCase() : null;
        city = city != null ? city.trim() : null;
        number = number != null ? number.trim() : null;
        country = country != null ? country.trim().toUpperCase() : null;
    }

    /**
     * Verifica se pelo menos um campo foi informado para atualização.
     */
    public boolean hasUpdates() {
        return (zipCode != null && !zipCode.isBlank())
                || (state != null && !state.isBlank())
                || (city != null && !city.isBlank())
                || (number != null && !number.isBlank())
                || (country != null && !country.isBlank());
    }
}
