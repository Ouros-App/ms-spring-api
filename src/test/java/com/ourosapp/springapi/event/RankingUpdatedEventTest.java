package com.ourosapp.springapi.event;

import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RankingUpdatedEventTest {

    @Test
    @DisplayName("RankingUpdatedEvent - Deve criar evento com lista imutavel e campos corretos")
    void deveCriarEventoComCamposCorretos() {
        FarmRankingResponseDTO dto = new FarmRankingResponseDTO(
                1L, "Fazenda Teste", "Sudeste", 1.95, 1L
        );
        RankingUpdatedEvent event = new RankingUpdatedEvent(10L, List.of(dto));

        assertEquals(10L, event.enterpriseId());
        assertEquals(1, event.rankings().size());
        assertEquals("Fazenda Teste", event.rankings().get(0).farmName());
    }

    @Test
    @DisplayName("RankingUpdatedEvent - Deve inicializar com lista vazia quando rankings for nulo")
    void deveInicializarComListaVaziaQuandoRankingsNulo() {
        RankingUpdatedEvent event = new RankingUpdatedEvent(null, null);

        assertNull(event.enterpriseId());
        assertNotNull(event.rankings());
        assertTrue(event.rankings().isEmpty());
    }
}
