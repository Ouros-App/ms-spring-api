package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.farm.FarmGoalsProgressResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmWaterConsumptionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para DTOs analíticos e de métricas de fazenda.
 */
class FarmMetricsDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("FarmWaterConsumptionResponseDTO - Deve instanciar e serializar corretamente para snake_case")
    void deveSerializarFarmWaterConsumptionCorretamente() throws Exception {
        FarmWaterConsumptionResponseDTO dto = new FarmWaterConsumptionResponseDTO(10L, new BigDecimal("150.50"));

        assertEquals(10L, dto.idFarm());
        assertEquals(new BigDecimal("150.50"), dto.waterConsumption());

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"id_farm\":10"));
        assertTrue(json.contains("\"water_consumption\":150.50"));

        FarmWaterConsumptionResponseDTO deserialized = objectMapper.readValue(json, FarmWaterConsumptionResponseDTO.class);
        assertEquals(dto.idFarm(), deserialized.idFarm());
        assertEquals(dto.waterConsumption(), deserialized.waterConsumption());
    }

    @Test
    @DisplayName("FarmGoalsProgressResponseDTO - Deve instanciar e serializar corretamente para snake_case")
    void deveSerializarFarmGoalsProgressCorretamente() throws Exception {
        FarmGoalsProgressResponseDTO dto = new FarmGoalsProgressResponseDTO(20L, new BigDecimal("85.75"));

        assertEquals(20L, dto.idFarm());
        assertEquals(new BigDecimal("85.75"), dto.goalsProgressPercentage());

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"id_farm\":20"));
        assertTrue(json.contains("\"goals_progress_percentage\":85.75"));

        FarmGoalsProgressResponseDTO deserialized = objectMapper.readValue(json, FarmGoalsProgressResponseDTO.class);
        assertEquals(dto.idFarm(), deserialized.idFarm());
        assertEquals(dto.goalsProgressPercentage(), deserialized.goalsProgressPercentage());
    }
}
