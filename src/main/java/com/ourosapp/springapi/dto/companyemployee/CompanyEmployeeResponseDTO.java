package com.ourosapp.springapi.dto.companyemployee;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.CompanyEmployee;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * DTO de resposta contendo as informações do Funcionário da Empresa Integradora (sem expor credenciais sensíveis).
 *
 * @param id             Identificador único do funcionário
 * @param name           Nome completo do funcionário
 * @param documentNumber Documento/CPF do funcionário (serializado como document_number)
 * @param email          E-mail corporativo do funcionário
 * @param telephone      Telefone de contato
 * @param idEnterprise   Identificador da empresa vinculada (serializado como id_enterprise)
 */
@Schema(description = "Resposta contendo os dados do Funcionário da Empresa Integradora")
public record CompanyEmployeeResponseDTO(

        @Schema(description = "Identificador único do funcionário", example = "1")
        Long id,

        @Schema(description = "Nome completo do funcionário", example = "João da Silva")
        String name,

        @Schema(description = "Documento/CPF do funcionário", example = "12345678901")
        @JsonProperty("document_number")
        String documentNumber,

        @Schema(description = "E-mail corporativo do funcionário", example = "joao.silva@empresa.com.br")
        String email,

        @Schema(description = "Telefone de contato", example = "11987654321")
        String telephone,

        @Schema(description = "Identificador da empresa integradora vinculada", example = "1")
        @JsonProperty("id_enterprise")
        Long idEnterprise
) {

    /**
     * Converte uma entidade {@link CompanyEmployee} em {@link CompanyEmployeeResponseDTO}.
     *
     * @param companyEmployee entidade do funcionário a ser convertida (não deve ser nula)
     * @return DTO correspondente sem dados sensíveis
     * @throws NullPointerException se companyEmployee for nulo
     */
    public static CompanyEmployeeResponseDTO fromEntity(CompanyEmployee companyEmployee) {
        Objects.requireNonNull(companyEmployee, "CompanyEmployee não pode ser nulo");
        return new CompanyEmployeeResponseDTO(
                companyEmployee.getId(),
                companyEmployee.getName(),
                companyEmployee.getDocumentNumber(),
                companyEmployee.getEmail(),
                companyEmployee.getTelephone(),
                companyEmployee.getIdEnterprise()
        );
    }
}
