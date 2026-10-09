package com.ourosapp.springapi.repository.projection;

/**
 * Interface projection para mapear os resultados calculados diretamente da consulta SQL nativa de ranqueamento.
 */
public interface FarmRankingProjection {

    Long getFarmId();

    String getFarmName();

    String getRegion();

    Integer getPoultryCapacity();

    Integer getChickensNow();

    Long getIdEnterprise();

    Double getCaa();

    Double getCea();

    Double getCgi();

    Long getRankPosition();

    Double getPercentilePct();

    String getMedal();
}
