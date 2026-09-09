package com.ourosapp.springapi.dto.farmowner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.entity.FarmOwner;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * DTO de resposta contendo as informações do Produtor Rural (sem expor credenciais sensíveis).
 *
 * @param id             Identificador único do produtor rural
 * @param name           Nome completo do produtor rural
 * @param documentNumber Documento/CPF do produtor (serializado como document_number)
 * @param email          E-mail de acesso do produtor
 * @param telephone      Telefone de contato
 * @param idFarm         Identificador da fazenda vinculada (serializado como id_farm)
 */
@Schema(description = "Resposta contendo os dados do Produtor Rural")
public record FarmOwnerResponseDTO(

        @Schema(description = "Identificador único do produtor rural", example = "1")
        Long id,

        @Schema(description = "Nome completo do produtor rural", example = "Sebastião da Silva")
        String name,

        @Schema(description = "Documento/CPF do produtor rural", example = "12345678901")
        @JsonProperty("document_number")
        String documentNumber,

        @Schema(description = "E-mail de acesso do produtor rural", example = "sebastiao.silva@fazenda.com.br")
        String email,

        @Schema(description = "Telefone de contato", example = "11987654321")
        String telephone,

        @Schema(description = "Identificador da fazenda vinculada", example = "1")
        @JsonProperty("id_farm")
        Long idFarm
) {

    /**
     * Converte uma entidade {@link FarmOwner} em {@link FarmOwnerResponseDTO}.
     *
     * @param farmOwner entidade do produtor a ser convertida (não deve ser nula)
     * @return DTO correspondente sem dados sensíveis
     * @throws NullPointerException se farmOwner for nulo
     */
    public static FarmOwnerResponseDTO fromEntity(FarmOwner farmOwner) {
        Objects.requireNonNull(farmOwner, "FarmOwner não pode ser nulo");
        return new FarmOwnerResponseDTO(
                farmOwner.getId(),
                farmOwner.getName(),
                farmOwner.getDocumentNumber(),
                farmOwner.getEmail(),
                farmOwner.getTelephone(),
                farmOwner.getIdFarm()
        );
    }
}
