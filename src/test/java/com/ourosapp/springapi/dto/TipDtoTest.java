package com.ourosapp.springapi.dto;

import com.ourosapp.springapi.dto.tip.TipRequestDTO;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.dto.tip.TipUpdateDTO;
import com.ourosapp.springapi.entity.Tip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para os DTOs do domínio de Dica Técnica.
 */
class TipDtoTest {

    @Test
    @DisplayName("TipRequestDTO - Deve sanitizar espaços no texto da dica")
    void deveSanitizarEspacosEmTipRequestDTO() {
        TipRequestDTO request = new TipRequestDTO("   Manter a ventilação ligada   ", 1L, List.of(10L, 20L));
        assertEquals("Manter a ventilação ligada", request.tip());
        assertEquals(1L, request.idFarm());
        assertEquals(List.of(10L, 20L), request.categoryIds());
    }

    @Test
    @DisplayName("TipResponseDTO - Deve instanciar corretamente a partir da entidade Tip")
    void deveInstanciarTipResponseDTOFromEntity() {
        Tip tip = Tip.builder()
                .id(1L)
                .tip("Manter temperatura ideal")
                .idFarm(5L)
                .build();

        TipResponseDTO response = TipResponseDTO.fromEntity(tip, List.of("Ambiência"), 3, 4.5);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Manter temperatura ideal", response.tip());
        assertEquals(5L, response.idFarm());
        assertEquals(List.of("Ambiência"), response.categories());
        assertEquals(3, response.totalReviews());
        assertEquals(4.5, response.averageRating());
    }

    @Test
    @DisplayName("TipResponseDTO - Deve retornar null se a entidade for nula")
    void deveRetornarNullQuandoEntidadeNula() {
        assertNull(TipResponseDTO.fromEntity(null, List.of(), 0, 0.0));
    }

    @Test
    @DisplayName("TipUpdateDTO - Deve detectar se há atualizações")
    void deveDetectarPresencaDeAtualizacoesEmTipUpdateDTO() {
        TipUpdateDTO emptyUpdate = new TipUpdateDTO(null, null);
        assertFalse(emptyUpdate.hasUpdates());

        TipUpdateDTO blankUpdate = new TipUpdateDTO("   ", null);
        assertFalse(blankUpdate.hasUpdates());

        TipUpdateDTO textUpdate = new TipUpdateDTO("   Novo texto   ", null);
        assertTrue(textUpdate.hasUpdates());
        assertEquals("Novo texto", textUpdate.tip());

        TipUpdateDTO categoryUpdate = new TipUpdateDTO(null, List.of(1L, 2L));
        assertTrue(categoryUpdate.hasUpdates());
    }
}
