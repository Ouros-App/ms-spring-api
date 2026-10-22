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
    @DisplayName("TipRequestDTO - Deve sanitizar espaços no texto da dica e aplicar valores padrão")
    void deveSanitizarEspacosEmTipRequestDTO() {
        TipRequestDTO request = new TipRequestDTO("   Manter a ventilação ligada   ", 1L, List.of(10L, 20L));
        assertEquals("Manter a ventilação ligada", request.tip());
        assertEquals("Dica Operacional", request.title());
        assertNull(request.fotoUrl());
        assertEquals(1L, request.idFarm());
        assertEquals(List.of(10L, 20L), request.categoryIds());
    }

    @Test
    @DisplayName("TipRequestDTO - Deve aceitar e sanitizar título e foto_url personalizados")
    void deveSanitizarTituloEFotoPersonalizadosEmTipRequestDTO() {
        TipRequestDTO request = new TipRequestDTO(
                "  Ventilação de Precisão  ",
                "   Manter a ventilação ligada   ",
                "  https://ouros.com/fotos/dica.png  ",
                1L,
                List.of(10L)
        );
        assertEquals("Ventilação de Precisão", request.title());
        assertEquals("Manter a ventilação ligada", request.tip());
        assertEquals("https://ouros.com/fotos/dica.png", request.fotoUrl());
        assertEquals(1L, request.idFarm());
    }

    @Test
    @DisplayName("TipRequestDTO - Deve aplicar título padrão quando fornecido nulo ou em branco")
    void deveAplicarTituloPadraoEmTipRequestDTO() {
        TipRequestDTO requestNullTitle = new TipRequestDTO(null, "Texto", "   ", 1L, List.of());
        assertEquals("Dica Operacional", requestNullTitle.title());
        assertNull(requestNullTitle.fotoUrl());

        TipRequestDTO requestBlankTitle = new TipRequestDTO("   ", "Texto", null, 1L, List.of());
        assertEquals("Dica Operacional", requestBlankTitle.title());
    }

    @Test
    @DisplayName("TipResponseDTO - Deve instanciar corretamente a partir da entidade Tip")
    void deveInstanciarTipResponseDTOFromEntity() {
        Tip tip = Tip.builder()
                .id(1L)
                .title("Controle Térmico")
                .tip("Manter temperatura ideal")
                .fotoUrl("https://ouros.com/banner.jpg")
                .build();

        TipResponseDTO response = TipResponseDTO.fromEntity(tip, 5L, List.of("Ambiência"), 3, 4.5);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Controle Térmico", response.title());
        assertEquals("Manter temperatura ideal", response.tip());
        assertEquals("https://ouros.com/banner.jpg", response.fotoUrl());
        assertEquals(5L, response.idFarm());
        assertEquals(List.of("Ambiência"), response.categories());
        assertEquals(3, response.totalReviews());
        assertEquals(4.5, response.averageRating());
    }

    @Test
    @DisplayName("TipResponseDTO - Deve retornar null se a entidade for nula")
    void deveRetornarNullQuandoEntidadeNula() {
        assertNull(TipResponseDTO.fromEntity(null, 5L, List.of(), 0, 0.0));
    }

    @Test
    @DisplayName("TipResponseDTO - Deve lidar com lista de categorias nula no fromEntity")
    void deveLidarComCategoriasNulasEmTipResponseDTO() {
        Tip tip = Tip.builder().id(1L).tip("Dica").build();
        TipResponseDTO response = TipResponseDTO.fromEntity(tip, 5L, null, 0, 0.0);
        assertNotNull(response);
        assertEquals("Dica Operacional", response.title());
        assertNull(response.fotoUrl());
        assertNotNull(response.categories());
        assertTrue(response.categories().isEmpty());
    }

    @Test
    @DisplayName("TipUpdateDTO - Deve detectar se há atualizações")
    void deveDetectarPresencaDeAtualizacoesEmTipUpdateDTO() {
        TipUpdateDTO emptyUpdate = new TipUpdateDTO(null, null, null, null);
        assertFalse(emptyUpdate.hasUpdates());

        TipUpdateDTO blankUpdate = new TipUpdateDTO("   ", "   ", null, null);
        assertFalse(blankUpdate.hasUpdates());

        TipUpdateDTO textUpdate = new TipUpdateDTO("   Novo texto   ", null);
        assertTrue(textUpdate.hasUpdates());
        assertEquals("Novo texto", textUpdate.tip());

        TipUpdateDTO titleUpdate = new TipUpdateDTO("  Novo Título  ", null, null, null);
        assertTrue(titleUpdate.hasUpdates());
        assertEquals("Novo Título", titleUpdate.title());

        TipUpdateDTO fotoUpdate = new TipUpdateDTO(null, null, "  https://ouros.com/nova.png  ", null);
        assertTrue(fotoUpdate.hasUpdates());
        assertEquals("https://ouros.com/nova.png", fotoUpdate.fotoUrl());

        TipUpdateDTO categoryUpdate = new TipUpdateDTO(null, List.of(1L, 2L));
        assertTrue(categoryUpdate.hasUpdates());
    }
}
