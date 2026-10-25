package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.tip.TipRequestDTO;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.dto.tip.TipUpdateDTO;
import com.ourosapp.springapi.entity.Tip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para os DTOs e entidades do domínio de Dica Técnica.
 */
class TipDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("TipRequestDTO - Deve sanitizar espaços no texto da dica e aplicar valores padrão")
    void deveSanitizarEspacosEmTipRequestDTO() {
        TipRequestDTO request = new TipRequestDTO("   Manter a ventilação ligada   ", 1L, List.of(10L, 20L));
        assertEquals("Manter a ventilação ligada", request.tip());
        assertEquals("Dica Operacional", request.title());
        assertNull(request.fotoUrl());
        assertNull(request.imageUrl());
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
        assertEquals("https://ouros.com/fotos/dica.png", request.imageUrl());
        assertEquals(1L, request.idFarm());
    }

    @Test
    @DisplayName("TipRequestDTO - Deve aceitar image_url Cloudinary em formato .webp e sincronizar com foto_url")
    void deveAceitarImageUrlCloudinaryEmTipRequestDTO() {
        String cloudinaryUrl = "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp";
        TipRequestDTO request = new TipRequestDTO(
                "Regulagem Diária de Nipple",
                "Verificar vazão diária de água.",
                cloudinaryUrl,
                null,
                1L,
                List.of(1L)
        );
        assertEquals(cloudinaryUrl, request.imageUrl());
        assertEquals(cloudinaryUrl, request.fotoUrl());
    }

    @Test
    @DisplayName("TipRequestDTO - Deve suportar textos longos formatados em Markdown sem perdas")
    void deveSuportarMarkdownEstruturadoComTopicosENegrito() {
        String markdownContent = """
                # Procedimento Operacional Padrão (POP)
                * **Fase 1:** Medir a pressão da água em todos os bicos.
                * **Fase 2:** Registrar no hidrômetro às 08h e 17h.
                1. Checar altura das linhas: bicos a 45 graus.
                2. Purgar as extremidades para eliminar biofilme.
                """;
        TipRequestDTO request = new TipRequestDTO(
                "POP de Hidratação de Aves",
                markdownContent,
                "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp",
                1L,
                List.of(1L)
        );
        assertEquals(markdownContent.trim(), request.tip());
    }

    @Test
    @DisplayName("TipRequestDTO - Deve aceitar títulos de até 200 caracteres")
    void deveAceitarTituloComAte200CaracteresEmTipRequestDTO() {
        String longTitle = "A".repeat(200);
        TipRequestDTO request = new TipRequestDTO(longTitle, "Conteúdo da dica", null, 1L, List.of());
        assertEquals(longTitle, request.title());
    }

    @Test
    @DisplayName("TipRequestDTO - Deve aplicar título padrão quando fornecido nulo ou em branco")
    void deveAplicarTituloPadraoEmTipRequestDTO() {
        TipRequestDTO requestNullTitle = new TipRequestDTO(null, "Texto", "   ", 1L, List.of());
        assertEquals("Dica Operacional", requestNullTitle.title());
        assertNull(requestNullTitle.fotoUrl());
        assertNull(requestNullTitle.imageUrl());

        TipRequestDTO requestBlankTitle = new TipRequestDTO("   ", "Texto", null, 1L, List.of());
        assertEquals("Dica Operacional", requestBlankTitle.title());
    }

    @Test
    @DisplayName("TipRequestDTO - Deve desserializar JSON com chave image_url ou foto_url corretamente")
    void deveDesserializarTipRequestDTOComVariantesDeImagem() throws Exception {
        String jsonImageUrl = """
                {
                    "title": "Dica Nipple",
                    "tip": "Instruções aqui",
                    "image_url": "https://res.cloudinary.com/img.webp",
                    "id_farm": 1
                }
                """;
        TipRequestDTO dto1 = objectMapper.readValue(jsonImageUrl, TipRequestDTO.class);
        assertEquals("https://res.cloudinary.com/img.webp", dto1.imageUrl());
        assertEquals("https://res.cloudinary.com/img.webp", dto1.fotoUrl());

        String jsonFotoUrl = """
                {
                    "title": "Dica Nipple",
                    "tip": "Instruções aqui",
                    "foto_url": "https://res.cloudinary.com/img2.webp",
                    "id_farm": 1
                }
                """;
        TipRequestDTO dto2 = objectMapper.readValue(jsonFotoUrl, TipRequestDTO.class);
        assertEquals("https://res.cloudinary.com/img2.webp", dto2.imageUrl());
        assertEquals("https://res.cloudinary.com/img2.webp", dto2.fotoUrl());
    }

    @Test
    @DisplayName("TipResponseDTO - Deve instanciar corretamente a partir da entidade Tip")
    void deveInstanciarTipResponseDTOFromEntity() {
        Tip tip = Tip.builder()
                .id(1L)
                .title("Controle Térmico")
                .tip("Manter temperatura ideal")
                .fotoUrl("https://ouros.com/banner.jpg")
                .imageUrl("https://ouros.com/banner.jpg")
                .build();

        TipResponseDTO response = TipResponseDTO.fromEntity(tip, 5L, List.of("Ambiência"), 3, 4.5);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Controle Térmico", response.title());
        assertEquals("Manter temperatura ideal", response.tip());
        assertEquals("https://ouros.com/banner.jpg", response.fotoUrl());
        assertEquals("https://ouros.com/banner.jpg", response.imageUrl());
        assertEquals(5L, response.idFarm());
        assertEquals(List.of("Ambiência"), response.categories());
        assertEquals(3, response.totalReviews());
        assertEquals(4.5, response.averageRating());
    }

    @Test
    @DisplayName("TipResponseDTO - Deve preencher ambas URLs quando apenas imageUrl estiver presente na entidade")
    void devePreencherAmbasUrlsQuandoApenasImageUrlEstiverPresente() {
        Tip tip = Tip.builder()
                .id(2L)
                .title("Biosseguridade")
                .tip("Desinfetar rodas na entrada")
                .imageUrl("https://res.cloudinary.com/tips/bio.webp")
                .build();

        TipResponseDTO response = TipResponseDTO.fromEntity(tip, 1L, List.of(), 0, 0.0);
        assertEquals("https://res.cloudinary.com/tips/bio.webp", response.imageUrl());
        assertEquals("https://res.cloudinary.com/tips/bio.webp", response.fotoUrl());
    }

    @Test
    @DisplayName("TipResponseDTO - Deve serializar contendo simultaneamente image_url e foto_url no JSON")
    void deveSerializarContendoAmbasChavesDeImagemNoJson() throws Exception {
        TipResponseDTO response = new TipResponseDTO(
                1L,
                "Título Técnico",
                "Texto em Markdown: **Importante**",
                "https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp",
                10L,
                List.of("Manejo"),
                1,
                5.0
        );

        String json = objectMapper.writeValueAsString(response);
        JsonNode jsonNode = objectMapper.readTree(json);

        assertTrue(jsonNode.hasNonNull("image_url"), "Deve conter chave 'image_url' serializada");
        assertTrue(jsonNode.hasNonNull("foto_url"), "Deve conter chave 'foto_url' serializada para retrocompatibilidade");
        assertEquals("https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp", jsonNode.get("image_url").asText());
        assertEquals("https://res.cloudinary.com/mpyey2bi/image/upload/v1729606821/ouros/tips/img01.webp", jsonNode.get("foto_url").asText());
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
        assertNull(response.imageUrl());
        assertNotNull(response.categories());
        assertTrue(response.categories().isEmpty());
    }

    @Test
    @DisplayName("TipUpdateDTO - Deve detectar se há atualizações incluindo image_url")
    void deveDetectarPresencaDeAtualizacoesEmTipUpdateDTO() {
        TipUpdateDTO emptyUpdate = new TipUpdateDTO(null, null, null, null, null);
        assertFalse(emptyUpdate.hasUpdates());

        TipUpdateDTO blankUpdate = new TipUpdateDTO("   ", "   ", null, null, null);
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
        assertEquals("https://ouros.com/nova.png", fotoUpdate.imageUrl());

        TipUpdateDTO imageUpdate = new TipUpdateDTO(null, null, "https://res.cloudinary.com/img.webp", null, null);
        assertTrue(imageUpdate.hasUpdates());
        assertEquals("https://res.cloudinary.com/img.webp", imageUpdate.imageUrl());
        assertEquals("https://res.cloudinary.com/img.webp", imageUpdate.fotoUrl());

        TipUpdateDTO categoryUpdate = new TipUpdateDTO(null, List.of(1L, 2L));
        assertTrue(categoryUpdate.hasUpdates());
    }

    @Test
    @DisplayName("Tip - Deve sincronizar imageUrl e fotoUrl no ciclo de vida da entidade")
    void deveSincronizarUrlsDeImagemNoCicloDeVidaDeTip() {
        Tip tipWithImage = Tip.builder().imageUrl("https://cloudinary.com/img1.webp").build();
        tipWithImage.syncImageUrls();
        assertEquals("https://cloudinary.com/img1.webp", tipWithImage.getFotoUrl());

        Tip tipWithFoto = Tip.builder().fotoUrl("https://cloudinary.com/img2.webp").build();
        tipWithFoto.syncImageUrls();
        assertEquals("https://cloudinary.com/img2.webp", tipWithFoto.getImageUrl());
    }
}
