package com.ourosapp.springapi.repository.projection;

import java.time.LocalDate;

/**
 * Projeção Spring Data JPA para mapeamento consolidado de registros de produção e lotes.
 */
public interface ProductionRegistryProjection {
    Long getId();
    String getIdLote();
    LocalDate getDataEmissao();
    Double getHidrometro();
    Double getCusto();
    Integer getEntraram();
    Integer getSairam();
    Integer getGalinhasAtuais();
    Long getIdFarm();
    String getFarmName();
}
