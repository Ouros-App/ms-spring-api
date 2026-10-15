package com.ourosapp.springapi.event;

import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;

import java.util.List;

/**
 * Evento disparado quando o ranking de fazendas de uma empresa integradora é recalculado e atualizado.
 *
 * @param enterpriseId identificador da empresa integradora
 * @param rankings lista consolidada do ranking das fazendas
 */
public record RankingUpdatedEvent(
        Long enterpriseId,
        List<FarmRankingResponseDTO> rankings
) {
    public RankingUpdatedEvent {
        rankings = rankings != null ? List.copyOf(rankings) : List.of();
    }
}
