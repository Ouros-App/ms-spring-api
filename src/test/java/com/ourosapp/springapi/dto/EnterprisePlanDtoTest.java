package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.enterprise.EnterpriseResponseDTO;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanRequestDTO;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanResponseDTO;
import com.ourosapp.springapi.dto.plan.PlanResponseDTO;
import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.entity.EnterprisePlan;
import com.ourosapp.springapi.entity.Plan;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para os DTOs e entidade do domínio de Planos da Empresa (EnterprisePlan).
 */
class EnterprisePlanDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("EnterprisePlanRequestDTO - Deve validar DTO com campos válidos")
    void deveValidarEnterprisePlanRequestDTOValido() {
        EnterprisePlanRequestDTO request = new EnterprisePlanRequestDTO(1L, 2L);
        assertEquals(1L, request.idEnterprise());
        assertEquals(2L, request.idPlan());

        Set<ConstraintViolation<EnterprisePlanRequestDTO>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("EnterprisePlanRequestDTO - Deve falhar quando idPlan for nulo ou não positivo")
    void deveFalharQuandoIdPlanInvalido() {
        EnterprisePlanRequestDTO nullPlan = new EnterprisePlanRequestDTO(1L, null);
        Set<ConstraintViolation<EnterprisePlanRequestDTO>> violationsNull = validator.validate(nullPlan);
        assertFalse(violationsNull.isEmpty());

        EnterprisePlanRequestDTO negativePlan = new EnterprisePlanRequestDTO(1L, -5L);
        Set<ConstraintViolation<EnterprisePlanRequestDTO>> violationsNegative = validator.validate(negativePlan);
        assertFalse(violationsNegative.isEmpty());
    }

    @Test
    @DisplayName("EnterprisePlanRequestDTO - Deve desserializar snake_case e camelCase")
    void deveDesserializarSnakeECamelCase() throws JsonProcessingException {
        String snakeJson = """
                {
                    "id_enterprise": 10,
                    "id_plan": 20
                }
                """;
        EnterprisePlanRequestDTO fromSnake = objectMapper.readValue(snakeJson, EnterprisePlanRequestDTO.class);
        assertEquals(10L, fromSnake.idEnterprise());
        assertEquals(20L, fromSnake.idPlan());

        String camelJson = """
                {
                    "idEnterprise": 10,
                    "idPlan": 20
                }
                """;
        EnterprisePlanRequestDTO fromCamel = objectMapper.readValue(camelJson, EnterprisePlanRequestDTO.class);
        assertEquals(10L, fromCamel.idEnterprise());
        assertEquals(20L, fromCamel.idPlan());
    }

    @Test
    @DisplayName("EnterprisePlanResponseDTO - Deve instanciar corretamente a partir da entidade EnterprisePlan")
    void deveInstanciarResponseDTOFromEntity() {
        EnterprisePlan entity = EnterprisePlan.builder()
                .id(1L)
                .idEnterprise(10L)
                .idPlan(20L)
                .build();

        EnterprisePlanResponseDTO response = EnterprisePlanResponseDTO.fromEntity(entity);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.idEnterprise());
        assertEquals(20L, response.idPlan());
        assertNull(response.planDetails());
        assertNull(response.enterpriseDetails());
    }

    @Test
    @DisplayName("EnterprisePlanResponseDTO - Deve retornar null se a entidade for nula")
    void deveRetornarNullQuandoEntidadeNula() {
        assertNull(EnterprisePlanResponseDTO.fromEntity(null));
        assertNull(EnterprisePlanResponseDTO.fromEntityWithDetails(null, null, null));
    }

    @Test
    @DisplayName("EnterprisePlanResponseDTO - Deve instanciar com detalhes completos")
    void deveInstanciarComDetalhesCompletos() {
        EnterprisePlan entity = EnterprisePlan.builder()
                .id(1L)
                .idEnterprise(10L)
                .idPlan(20L)
                .build();

        Plan plan = Plan.builder()
                .id(20L)
                .title("Plano Premium")
                .durationDays(365)
                .description("Desc")
                .price(new BigDecimal("2990.00"))
                .build();
        PlanResponseDTO planDetails = PlanResponseDTO.fromEntity(plan);

        Enterprise enterprise = Enterprise.builder()
                .id(10L)
                .name("Empresa Teste")
                .email("contato@teste.com")
                .documentNumber("12345678000195")
                .telephone("11999999999")
                .idAddress(1L)
                .build();
        EnterpriseResponseDTO enterpriseDetails = EnterpriseResponseDTO.fromEntity(enterprise);

        EnterprisePlanResponseDTO response = EnterprisePlanResponseDTO.fromEntityWithDetails(
                entity, planDetails, enterpriseDetails
        );

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.idEnterprise());
        assertEquals(20L, response.idPlan());
        assertNotNull(response.planDetails());
        assertEquals("Plano Premium", response.planDetails().title());
        assertNotNull(response.enterpriseDetails());
        assertEquals("Empresa Teste", response.enterpriseDetails().name());
    }

    @Test
    @DisplayName("EnterprisePlan - Deve verificar getters, setters, builder e toString da entidade")
    void deveTestarEntidadeEnterprisePlan() {
        EnterprisePlan ep = new EnterprisePlan();
        ep.setId(1L);
        ep.setIdEnterprise(2L);
        ep.setIdPlan(3L);

        assertEquals(1L, ep.getId());
        assertEquals(2L, ep.getIdEnterprise());
        assertEquals(3L, ep.getIdPlan());
        assertTrue(ep.toString().contains("idEnterprise=2"));

        EnterprisePlan built = EnterprisePlan.builder()
                .id(10L)
                .idEnterprise(20L)
                .idPlan(30L)
                .build();
        assertEquals(10L, built.getId());
        assertEquals(20L, built.getIdEnterprise());
        assertEquals(30L, built.getIdPlan());
    }
}
