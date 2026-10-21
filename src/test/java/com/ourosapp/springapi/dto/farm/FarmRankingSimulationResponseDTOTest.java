package com.ourosapp.springapi.dto.farm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes unitários para FarmRankingSimulationResponseDTO")
class FarmRankingSimulationResponseDTOTest {

    @Test
    @DisplayName("Deve instanciar DTO com valores corretos e sanitizar strings")
    void deveInstanciarDTOComSucesso() {
        FarmRankingSimulationResponseDTO dto = new FarmRankingSimulationResponseDTO(
                1L,
                "  Granja São Judas Tadeu  ",
                7.85,
                12L,
                "  bronze  ",
                6.07,
                4L,
                "  ouro  ",
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

    @Test
    @DisplayName("Deve ajustar positionsGained para zero se for nulo ou negativo")
    void deveAjustarPositionsGainedQuandoInvalido() {
        FarmRankingSimulationResponseDTO dtoNull = new FarmRankingSimulationResponseDTO(
                1L, null, 7.85, 12L, null, 6.07, 15L, null, null, 25L, 60.0
        );
        assertEquals(0L, dtoNull.positionsGained());
        assertNull(dtoNull.farmName());
        assertNull(dtoNull.currentLeague());
        assertNull(dtoNull.simulatedLeague());

        FarmRankingSimulationResponseDTO dtoNegative = new FarmRankingSimulationResponseDTO(
                1L, "Granja", 7.85, 12L, "BRONZE", 8.50, 15L, "BRONZE", -3L, 25L, 60.0
        );
        assertEquals(0L, dtoNegative.positionsGained());
    }
}
