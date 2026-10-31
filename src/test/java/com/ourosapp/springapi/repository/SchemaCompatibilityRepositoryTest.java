package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.WaterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita persistência real sem instalar funções ou procedures no banco de teste. */
@DataJpaTest
class SchemaCompatibilityRepositoryTest {
    @Autowired StateGoalRepository goals;
    @Autowired FarmGoalRepository links;
    @Autowired RegionGoalRepository regions;
    @Autowired StateGoalRegionRepository regionLinks;
    @Autowired WaterRegistryRepository water;

    @Test
    void devePersistirMetaEVinculosSemProcedure() {
        Integer id = goals.callCreateStateGoal("Economizar água", "Meta de teste", "WATER_CONSUMPTION",
                "COMPLETED", new BigDecimal("10"), LocalDateTime.of(2026, 1, 1, 0, 0), null, 7, "Sudeste");
        assertTrue(goals.findById(id.longValue()).isPresent());
        assertEquals("COMPLETED", links.findByIdFarmAndIdGoal(7L, id.longValue()).orElseThrow().getStatus());
        assertEquals(1, regions.findByIdGoal(id.longValue()).size());
        assertEquals(1, regionLinks.findByIdGoal(id.longValue()).size());
        assertEquals(0, new BigDecimal("100").compareTo(goals.getGoalsProgress(7L)));
        assertEquals(0, BigDecimal.ZERO.compareTo(goals.getGoalsProgress(8L)));
    }

    @Test
    void deveUsarSomenteAMedicaoMaisRecenteComDesempatePorId() {
        water.saveAndFlush(WaterRegistry.builder().idFarm(7L).registrationDate(LocalDate.of(2026, 1, 1))
                .startHydrometer(new BigDecimal("10")).endHydrometer(new BigDecimal("20")).build());
        water.saveAndFlush(WaterRegistry.builder().idFarm(7L).registrationDate(LocalDate.of(2026, 1, 2))
                .startHydrometer(new BigDecimal("20")).endHydrometer(new BigDecimal("23")).build());
        water.saveAndFlush(WaterRegistry.builder().idFarm(7L).registrationDate(LocalDate.of(2026, 1, 2))
                .startHydrometer(new BigDecimal("23")).endHydrometer(new BigDecimal("27")).build());
        assertEquals(0, new BigDecimal("4").compareTo(water.getLatestWaterConsumption(7L)));
        assertEquals(0, BigDecimal.ZERO.compareTo(water.getLatestWaterConsumption(8L)));
    }
}
