package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.farm.FarmGoalsProgressResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmWaterConsumptionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes unitários para DTOs analíticos e de métricas de fazenda.
 */
class FarmMetricsDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve serializar e desserializar FarmWaterConsumptionResponseDTO em snake_case")
    void deveValidarContratoFarmWaterConsumption() throws Exception {
        var dto = new FarmWaterConsumptionResponseDTO(10L, new BigDecimal("150.50"));
        String json = objectMapper.writeValueAsString(dto);
        var parsed = objectMapper.readValue(json, FarmWaterConsumptionResponseDTO.class);

        assertEquals(dto, parsed);
        assertEquals(new BigDecimal("150.50"), parsed.waterConsumption());
    }

    @Test
    @DisplayName("Deve serializar e desserializar FarmGoalsProgressResponseDTO em snake_case")
    void deveValidarContratoFarmGoalsProgress() throws Exception {
        var dto = new FarmGoalsProgressResponseDTO(20L, new BigDecimal("85.75"));
        String json = objectMapper.writeValueAsString(dto);
        var parsed = objectMapper.readValue(json, FarmGoalsProgressResponseDTO.class);

        assertEquals(dto, parsed);
        assertEquals(new BigDecimal("85.75"), parsed.goalsProgressPercentage());
    }

    @Test
    @DisplayName("Deve retornar a própria instância em FarmRankingResponseDTO quando countdown for nulo")
    void deveRetornarMesmaInstanciaQuandoCountdownNuloEmFarmRankingResponseDTO() {
        var dto = new com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO(
                1L, " Granja Esperança ", "Sudeste", 1.25, 1L
        );

        assertEquals("Granja Esperança", dto.farmName());
        var updated = dto.withCountdown(null);
        org.junit.jupiter.api.Assertions.assertSame(dto, updated);

        var countdown = new com.ourosapp.springapi.dto.farm.RankingCountdownDTO(
                3600L, "1 hora", "2026-10-11T00:00:00-03:00"
        );
        var withCountdown = dto.withCountdown(countdown);
        org.junit.jupiter.api.Assertions.assertNotSame(dto, withCountdown);
        assertEquals(3600L, withCountdown.secondsUntilNextReset());
        assertEquals("1 hora", withCountdown.timeUntilNextReset());
        assertEquals("2026-10-11T00:00:00-03:00", withCountdown.nextResetAt());
    }

    @Test
    @DisplayName("Deve retornar a própria instância em FarmPodiumRankingResponseDTO quando countdown for nulo")
    void deveRetornarMesmaInstanciaQuandoCountdownNuloEmFarmPodiumRankingResponseDTO() {
        var item = new com.ourosapp.springapi.dto.farm.FarmPodiumItemDTO(
                1L, "Granja 1", "Sul", 1.5, 1L, "CURRENT"
        );
        var podium = new com.ourosapp.springapi.dto.farm.FarmPodiumRankingResponseDTO(
                1L, java.util.List.of(item)
        );

        var updated = podium.withCountdown(null);
        org.junit.jupiter.api.Assertions.assertSame(podium, updated);

        var countdown = new com.ourosapp.springapi.dto.farm.RankingCountdownDTO(
                7200L, "2 horas", "2026-10-11T00:00:00-03:00"
        );
        var withCountdown = podium.withCountdown(countdown);
        org.junit.jupiter.api.Assertions.assertNotSame(podium, withCountdown);
        assertEquals(7200L, withCountdown.secondsUntilNextReset());
        assertEquals("2 horas", withCountdown.timeUntilNextReset());
        assertEquals("2026-10-11T00:00:00-03:00", withCountdown.nextResetAt());
    }
}
