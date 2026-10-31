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
    @Autowired IndividualGoalRepository individualGoals;

    @Test
    void devePersistirMetaEVinculosSemProcedure() {
        Integer id = goals.callCreateStateGoal("Economizar água", "Meta de teste", "WATER_CONSUMPTION",
                "Concluída", new BigDecimal("10"), LocalDateTime.of(2026, 1, 1, 0, 0), null, 7, "Sudeste");
        assertTrue(goals.findById(id.longValue()).isPresent());
        assertEquals("Concluída", links.findByIdFarmAndIdGoal(7L, id.longValue()).orElseThrow().getStatus());
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
    @Test
    void deveCriarMetaGlobalSemVinculosEComDataFinal() {
        Integer id = goals.callCreateStateGoal("Global", null, "WATER_CONSUMPTION", null,
                BigDecimal.ONE, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0), null, null);
        assertEquals(LocalDate.of(2026, 2, 1), goals.findById(id.longValue()).orElseThrow().getDateEnd());
        assertTrue(links.findByIdGoal(id.longValue()).isEmpty());
        assertTrue(regions.findByIdGoal(id.longValue()).isEmpty());
    }

    @Test
    void deveUsarStatusPendenteSemRegiaoParaVinculoDeFazenda() {
        for (String status : new String[] { null, "", "Pendente" }) {
            Integer id = goals.callCreateStateGoal("Regional", null, "WATER_CONSUMPTION", status,
                    BigDecimal.ONE, LocalDateTime.of(2026, 1, 1, 0, 0), null, 7, "");
            assertEquals("Pendente", links.findByIdFarmAndIdGoal(7L, id.longValue()).orElseThrow().getStatus());
            assertTrue(regions.findByIdGoal(id.longValue()).isEmpty());
        }
    }
    @Test
    void deveCombinarMetasIndividuaisEEstaduaisComProgressoParcial() {
        goals.callCreateStateGoal("Estadual", null, "WATER_CONSUMPTION", "Em andamento",
                BigDecimal.ONE, LocalDateTime.of(2026, 1, 1, 0, 0), null, 7, null);
        individualGoals.saveAndFlush(com.ourosapp.springapi.entity.IndividualGoal.builder()
                .idFarm(7L).title("Individual").type("WATER_CONSUMPTION").status("Concluída")
                .targetValue(BigDecimal.ONE).build());
        assertEquals(0, new BigDecimal("75").compareTo(goals.getGoalsProgress(7L)));
        individualGoals.saveAndFlush(com.ourosapp.springapi.entity.IndividualGoal.builder()
                .idFarm(7L).title("Pendente").type("WATER_CONSUMPTION").status("Pendente")
                .targetValue(BigDecimal.ONE).build());
        assertEquals(0, new BigDecimal("50").compareTo(goals.getGoalsProgress(7L)));
    }
}
