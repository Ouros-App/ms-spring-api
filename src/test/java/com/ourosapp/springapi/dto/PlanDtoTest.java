package com.ourosapp.springapi.dto;

import com.ourosapp.springapi.dto.plan.PlanRequestDTO;
import com.ourosapp.springapi.dto.plan.PlanResponseDTO;
import com.ourosapp.springapi.dto.plan.PlanUpdateDTO;
import com.ourosapp.springapi.entity.Plan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para os DTOs do domínio de Planos de Assinatura.
 */
class PlanDtoTest {

    @Test
    @DisplayName("PlanRequestDTO - Deve sanitizar espaços em branco no título e descrição")
    void deveSanitizarEspacosEmPlanRequestDTO() {
        PlanRequestDTO request = new PlanRequestDTO(
                "   Plano Ouro Anual   ",
                365,
                "   Descrição completa com espaços extras   ",
                new BigDecimal("2990.00")
        );

        assertEquals("Plano Ouro Anual", request.title());
        assertEquals(365, request.durationDays());
        assertEquals("Descrição completa com espaços extras", request.description());
        assertEquals(new BigDecimal("2990.00"), request.price());
    }

    @Test
    @DisplayName("PlanRequestDTO - Deve manter campos nulos quando fornecidos como nulo")
    void deveManterCamposNulosEmPlanRequestDTO() {
        PlanRequestDTO request = new PlanRequestDTO(null, null, null, null);
        assertNull(request.title());
        assertNull(request.durationDays());
        assertNull(request.description());
        assertNull(request.price());
    }

    @Test
    @DisplayName("PlanResponseDTO - Deve instanciar corretamente a partir da entidade Plan")
    void deveInstanciarPlanResponseDTOFromEntity() {
        Plan plan = Plan.builder()
                .id(1L)
                .title("Plano Safra Premium")
                .durationDays(180)
                .description("Acesso completo à gestão de granjas")
                .price(new BigDecimal("1500.00"))
                .build();

        PlanResponseDTO response = PlanResponseDTO.fromEntity(plan);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Plano Safra Premium", response.title());
        assertEquals(180, response.durationDays());
        assertEquals("Acesso completo à gestão de granjas", response.description());
        assertEquals(new BigDecimal("1500.00"), response.price());
    }

    @Test
    @DisplayName("PlanResponseDTO - Deve retornar null se a entidade for nula")
    void deveRetornarNullQuandoEntidadePlanNula() {
        assertNull(PlanResponseDTO.fromEntity(null));
    }

    @Test
    @DisplayName("PlanUpdateDTO - Deve sanitizar campos de texto")
    void deveSanitizarCamposEmPlanUpdateDTO() {
        PlanUpdateDTO update = new PlanUpdateDTO(
                "   Plano Atualizado   ",
                90,
                "   Nova descrição   ",
                new BigDecimal("800.00")
        );

        assertEquals("Plano Atualizado", update.title());
        assertEquals(90, update.durationDays());
        assertEquals("Nova descrição", update.description());
        assertEquals(new BigDecimal("800.00"), update.price());
    }

    @Test
    @DisplayName("PlanUpdateDTO - Deve retornar true em hasUpdates quando houver ao menos um campo preenchido")
    void deveRetornarTrueEmHasUpdatesQuandoHouverCampoPreenchido() {
        PlanUpdateDTO updateTitleOnly = new PlanUpdateDTO("Novo Título", null, null, null);
        assertTrue(updateTitleOnly.hasUpdates());

        PlanUpdateDTO updateDurationOnly = new PlanUpdateDTO(null, 60, null, null);
        assertTrue(updateDurationOnly.hasUpdates());

        PlanUpdateDTO updateDescriptionOnly = new PlanUpdateDTO(null, null, "Desc", null);
        assertTrue(updateDescriptionOnly.hasUpdates());

        PlanUpdateDTO updatePriceOnly = new PlanUpdateDTO(null, null, null, new BigDecimal("100.00"));
        assertTrue(updatePriceOnly.hasUpdates());
    }

    @Test
    @DisplayName("PlanUpdateDTO - Deve retornar false em hasUpdates quando todos os campos forem nulos")
    void deveRetornarFalseEmHasUpdatesQuandoTodosCamposNulos() {
        PlanUpdateDTO emptyUpdate = new PlanUpdateDTO(null, null, null, null);
        assertFalse(emptyUpdate.hasUpdates());
    }
}
