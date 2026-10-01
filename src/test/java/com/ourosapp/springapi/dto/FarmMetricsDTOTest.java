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
}
