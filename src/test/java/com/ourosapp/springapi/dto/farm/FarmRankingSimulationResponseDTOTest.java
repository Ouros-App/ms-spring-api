package com.ourosapp.springapi.dto.farm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários para FarmRankingSimulationResponseDTO")
class FarmRankingSimulationResponseDTOTest {

    @Test
    @DisplayName("Deve instanciar DTO com valores corretos")
    void deveInstanciarDTOComSucesso() {
        FarmRankingSimulationResponseDTO dto = new FarmRankingSimulationResponseDTO(
                1L,
                "Granja São Judas Tadeu",
                7.85,
                12L,
                "BRONZE",
                6.07,
                4L,
                "OURO",
                8L,
                25L,
                16.0
        );

        assertEquals(1L, dto.farmId());
        assertEquals("Granja São Judas Tadeu", dto.farmName());
        assertEquals(7.85, dto.currentScore());
        assertEquals(12L, dto.currentPosition());
        assertEquals("BRONZE", dto.currentLeague());
        assertEquals(6.07, dto.simulatedScore());
        assertEquals(4L, dto.simulatedPosition());
        assertEquals("OURO", dto.simulatedLeague());
        assertEquals(8L, dto.positionsGained());
        assertEquals(25L, dto.totalFarms());
        assertEquals(16.0, dto.simulatedPercentilePct());
    }
}
