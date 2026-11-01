package com.ourosapp.springapi.dto.lot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ourosapp.springapi.repository.projection.ProductionRegistryProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO de resposta consolidada para registros de produção avícola, lotes e telemetria hídrica (BFF).
 */
@Schema(description = "Resumo consolidado de produção avícola, lotes e telemetria hídrica")
public record ProductionRegistryResponseDTO(

        @Schema(description = "Identificador único do lote", example = "1")
        @JsonProperty("id")
        Long id,

        @Schema(description = "Código de identificação do lote", example = "L-1")
        @JsonProperty("id_lote")
        @JsonAlias("idLote")
        String idLote,

        @Schema(description = "Data de emissão / registro", example = "2026-08-01")
        @JsonProperty("data_emissao")
        @JsonAlias("dataEmissao")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataEmissao,

        @Schema(description = "Leitura ou consumo acumulado do hidrômetro em m³", example = "7000.0")
        @JsonProperty("hidrometro")
        Double hidrometro,

        @Schema(description = "Custo operacional total em R$", example = "6000.00")
        @JsonProperty("custo")
        Double custo,

        @Schema(description = "Aves que entraram / alojadas", example = "6500")
        @JsonProperty("entraram")
        Integer entraram,

        @Schema(description = "Aves que saíram / baixas / abate", example = "500")
        @JsonProperty("sairam")
        Integer sairam,

        @Schema(description = "Saldo de aves vivas atuais", example = "6000")
        @JsonProperty("galinhas_atuais")
        @JsonAlias("galinhasAtuais")
        Integer galinhasAtuais,

        @Schema(description = "Identificador da fazenda", example = "10")
        @JsonProperty("id_farm")
        @JsonAlias("idFarm")
        Long idFarm,

        @Schema(description = "Nome da fazenda", example = "Granja São Judas")
        @JsonProperty("farm_name")
        @JsonAlias("farmName")
        String farmName
) {

    /**
     * Converte uma projeção {@link ProductionRegistryProjection} em {@link ProductionRegistryResponseDTO}.
     *
     * @param projection projeção retornada pela consulta agregada
     * @return DTO correspondente
     */
    public static ProductionRegistryResponseDTO fromProjection(ProductionRegistryProjection projection) {
        Objects.requireNonNull(projection, "Projection não pode ser nula");
        return new ProductionRegistryResponseDTO(
                projection.getId(),
                projection.getIdLote(),
                projection.getDataEmissao(),
                projection.getHidrometro() != null ? projection.getHidrometro() : 0.0,
                projection.getCusto() != null ? projection.getCusto() : 0.0,
                projection.getEntraram() != null ? projection.getEntraram() : 0,
                projection.getSairam() != null ? projection.getSairam() : 0,
                projection.getGalinhasAtuais() != null ? projection.getGalinhasAtuais() : 0,
                projection.getIdFarm(),
                projection.getFarmName()
        );
    }
}
