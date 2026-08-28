package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para operações de cadastro e atualização total de endereços.
 *
 * @param zipCode CEP do endereço (mapeado para zip_code no JSON)
 * @param state   Unidade Federativa / Estado com 2 caracteres (ex: "SP")
 * @param city    Nome da cidade (até 100 caracteres)
 * @param number  Número do imóvel ou identificação do logradouro
 * @param country Código do país com 2 caracteres (ex: "BR")
 */
@Schema(description = "Dados do endereço para criação e alteração")
public record AddressRequestDTO(

        @Schema(description = "CEP do endereço", example = "12345678")
        @JsonProperty("zip_code")
        @JsonAlias("zipCode")
        @NotBlank(message = "O CEP não pode estar em branco")
        @Size(max = 50, message = "O CEP deve ter no máximo 50 caracteres")
        String zipCode,

        @Schema(description = "Estado do endereço (UF com 2 caracteres)", example = "SP")
        @NotBlank(message = "O estado não pode estar em branco")
        @Size(min = 2, max = 2, message = "O estado deve ter 2 caracteres")
        String state,

        @Schema(description = "Cidade do endereço", example = "Campinas")
        @NotBlank(message = "A cidade não pode estar em branco")
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String city,

        @Schema(description = "Número do endereço", example = "555")
        @NotBlank(message = "O número não pode estar em branco")
        @Size(max = 50, message = "O número deve ter no máximo 50 caracteres")
        String number,

        @Schema(description = "País do endereço (código com 2 caracteres)", example = "BR")
        @NotBlank(message = "O país não pode estar em branco")
        @Size(min = 2, max = 2, message = "O país deve ter 2 caracteres")
        String country
) { }

