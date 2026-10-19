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

        assertEquals(" Granja Esperança ", dto.farmName());
        var updated = dto.withCountdown(null);
        org.junit.jupiter.api.Assertions.assertSame(dto, updated);

        var rankingTime = new com.ourosapp.springapi.dto.farm.RankingTimeDTO(null, "1 hora", "0 minutos");
        var countdown = new com.ourosapp.springapi.dto.farm.RankingCountdownDTO(
                3600L, rankingTime, "2026-10-11T00:00:00-03:00"
        );
        var withCountdown = dto.withCountdown(countdown);
        org.junit.jupiter.api.Assertions.assertNotSame(dto, withCountdown);
        assertEquals(3600L, withCountdown.secondsUntilNextReset());
        assertEquals(rankingTime, withCountdown.time());
        assertEquals("1 hora", withCountdown.time().horas());
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

        var rankingTime = new com.ourosapp.springapi.dto.farm.RankingTimeDTO("1 dia", "2 horas", "30 minutos");
        var countdown = new com.ourosapp.springapi.dto.farm.RankingCountdownDTO(
                7200L, rankingTime, "2026-10-11T00:00:00-03:00"
        );
        var withCountdown = podium.withCountdown(countdown);
        org.junit.jupiter.api.Assertions.assertNotSame(podium, withCountdown);
        assertEquals(7200L, withCountdown.secondsUntilNextReset());
        assertEquals(rankingTime, withCountdown.time());
        assertEquals("1 dia", withCountdown.time().days());
        assertEquals("2 horas", withCountdown.time().hours());
        assertEquals("30 minutos", withCountdown.time().minutes());
        assertEquals("2026-10-11T00:00:00-03:00", withCountdown.nextResetAt());
    }

    @Test
    @DisplayName("Deve serializar RankingTimeDTO em JSON padronizado com hours, minutes e omitindo days se for nulo")
    void deveSerializarRankingTimeDTO() throws Exception {
        var timeSemDias = new com.ourosapp.springapi.dto.farm.RankingTimeDTO(null, "6 horas", "8 minutos");
        String jsonSemDias = objectMapper.writeValueAsString(timeSemDias);
        org.junit.jupiter.api.Assertions.assertFalse(jsonSemDias.contains("\"days\""));
        org.junit.jupiter.api.Assertions.assertTrue(jsonSemDias.contains("\"hours\":\"6 horas\""));
        org.junit.jupiter.api.Assertions.assertTrue(jsonSemDias.contains("\"minutes\":\"8 minutos\""));

        var timeComDias = new com.ourosapp.springapi.dto.farm.RankingTimeDTO("2 dias", "6 horas", "8 minutos");
        String jsonComDias = objectMapper.writeValueAsString(timeComDias);
        org.junit.jupiter.api.Assertions.assertTrue(jsonComDias.contains("\"days\":\"2 dias\""));
        org.junit.jupiter.api.Assertions.assertTrue(jsonComDias.contains("\"hours\":\"6 horas\""));
        org.junit.jupiter.api.Assertions.assertTrue(jsonComDias.contains("\"minutes\":\"8 minutos\""));

        // Desserialização com aliases em português
        String jsonLegadoPt = "{\"dias\":\"1 dia\",\"horas\":\"3 horas\",\"minutos\":\"15 minutos\"}";
        var parsed = objectMapper.readValue(jsonLegadoPt, com.ourosapp.springapi.dto.farm.RankingTimeDTO.class);
        assertEquals("1 dia", parsed.days());
        assertEquals("3 horas", parsed.hours());
        assertEquals("15 minutos", parsed.minutes());
    }

    @Test
    @DisplayName("Deve calcular RankingTimeDTO a partir de Duration corretamente")
    void deveCalcularRankingTimeAPartirDeDuration() {
        var duration = java.time.Duration.ofDays(2).plusHours(6).plusMinutes(8);
        var time = com.ourosapp.springapi.dto.farm.RankingTimeDTO.fromDuration(duration);
        assertEquals("2 dias", time.days());
        assertEquals("6 horas", time.hours());
        assertEquals("8 minutos", time.minutes());
        org.junit.jupiter.api.Assertions.assertNull(time.seconds());
        assertEquals("2 dias, 6 horas e 8 minutos", time.toHumanReadable());

        var durationMenorQueUmDia = java.time.Duration.ofHours(1).plusMinutes(1);
        var timeMenor = com.ourosapp.springapi.dto.farm.RankingTimeDTO.fromDuration(durationMenorQueUmDia);
        org.junit.jupiter.api.Assertions.assertNull(timeMenor.days());
        assertEquals("1 hora", timeMenor.hours());
        assertEquals("1 minuto", timeMenor.minutes());
        assertEquals("1 hora e 1 minuto", timeMenor.toHumanReadable());

        var durationSegundos = java.time.Duration.ofSeconds(45);
        var timeSegundos = com.ourosapp.springapi.dto.farm.RankingTimeDTO.fromDuration(durationSegundos);
        org.junit.jupiter.api.Assertions.assertNull(timeSegundos.days());
        assertEquals("0 horas", timeSegundos.hours());
        assertEquals("0 minutos", timeSegundos.minutes());
        assertEquals("45 segundos", timeSegundos.seconds());
        assertEquals("45 segundos", timeSegundos.toHumanReadable());
    }
}
