package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.dto.payment.PaymentRequestDTO;
import com.ourosapp.springapi.dto.payment.PaymentResponseDTO;
import com.ourosapp.springapi.entity.Payment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para os DTOs e entidade do domínio de Pagamentos (Payment).
 */
class PaymentDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("PaymentRequestDTO - Deve sanitizar o tipo de pagamento e validar campos com sucesso")
    void deveSanitizarEValidarPaymentRequestDTOComSucesso() {
        PaymentRequestDTO request = new PaymentRequestDTO(
                "  pix  ",
                new BigDecimal("2990.00"),
                1L,
                2L
        );

        assertEquals("PIX", request.type());
        assertEquals(new BigDecimal("2990.00"), request.value());
        assertEquals(1L, request.idEnterprise());
        assertEquals(2L, request.idEnterprisePlan());

        Set<ConstraintViolation<PaymentRequestDTO>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("PaymentRequestDTO - Deve falhar quando type for nulo ou em branco")
    void deveFalharQuandoTypeInvalido() {
        PaymentRequestDTO nullType = new PaymentRequestDTO(null, new BigDecimal("100.00"), 1L, 2L);
        assertFalse(validator.validate(nullType).isEmpty());

        PaymentRequestDTO blankType = new PaymentRequestDTO("   ", new BigDecimal("100.00"), 1L, 2L);
        assertFalse(validator.validate(blankType).isEmpty());
    }

    @Test
    @DisplayName("PaymentRequestDTO - Deve falhar quando valor for nulo, zero ou negativo")
    void deveFalharQuandoValorInvalido() {
        PaymentRequestDTO nullValue = new PaymentRequestDTO("PIX", null, 1L, 2L);
        assertFalse(validator.validate(nullValue).isEmpty());

        PaymentRequestDTO zeroValue = new PaymentRequestDTO("PIX", BigDecimal.ZERO, 1L, 2L);
        assertFalse(validator.validate(zeroValue).isEmpty());

        PaymentRequestDTO negativeValue = new PaymentRequestDTO("PIX", new BigDecimal("-10.00"), 1L, 2L);
        assertFalse(validator.validate(negativeValue).isEmpty());
    }

    @Test
    @DisplayName("PaymentRequestDTO - Deve falhar quando idEnterprisePlan for nulo ou não positivo")
    void deveFalharQuandoIdEnterprisePlanInvalido() {
        PaymentRequestDTO nullPlan = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 1L, null);
        assertFalse(validator.validate(nullPlan).isEmpty());

        PaymentRequestDTO negativePlan = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 1L, -1L);
        assertFalse(validator.validate(negativePlan).isEmpty());
    }

    @Test
    @DisplayName("PaymentRequestDTO - Deve desserializar snake_case e camelCase")
    void deveDesserializarSnakeECamelCase() throws JsonProcessingException {
        String snakeJson = """
                {
                    "type": "boleto",
                    "value": 1500.50,
                    "id_enterprise": 1,
                    "id_enterprise_plan": 3
                }
                """;
        PaymentRequestDTO fromSnake = objectMapper.readValue(snakeJson, PaymentRequestDTO.class);
        assertEquals("BOLETO", fromSnake.type());
        assertEquals(new BigDecimal("1500.50"), fromSnake.value());
        assertEquals(1L, fromSnake.idEnterprise());
        assertEquals(3L, fromSnake.idEnterprisePlan());

        String camelJson = """
                {
                    "type": "credit_card",
                    "value": 2000.00,
                    "idEnterprise": 2,
                    "idEnterprisePlan": 4
                }
                """;
        PaymentRequestDTO fromCamel = objectMapper.readValue(camelJson, PaymentRequestDTO.class);
        assertEquals("CREDIT_CARD", fromCamel.type());
        assertEquals(new BigDecimal("2000.00"), fromCamel.value());
        assertEquals(2L, fromCamel.idEnterprise());
        assertEquals(4L, fromCamel.idEnterprisePlan());
    }

    @Test
    @DisplayName("PaymentResponseDTO - Deve instanciar corretamente a partir da entidade Payment")
    void deveInstanciarResponseDTOFromEntity() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 10, 0, 0);
        Payment payment = Payment.builder()
                .id(1L)
                .type("PIX")
                .value(new BigDecimal("2990.00"))
                .dateCreation(now)
                .idEnterprise(10L)
                .idEnterprisePlan(20L)
                .build();

        PaymentResponseDTO response = PaymentResponseDTO.fromEntity(payment);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("PIX", response.type());
        assertEquals(new BigDecimal("2990.00"), response.value());
        assertEquals(now, response.dateCreation());
        assertEquals(10L, response.idEnterprise());
        assertEquals(20L, response.idEnterprisePlan());
    }

    @Test
    @DisplayName("PaymentResponseDTO - Deve retornar null se a entidade for nula")
    void deveRetornarNullQuandoEntidadeNula() {
        assertNull(PaymentResponseDTO.fromEntity(null));
    }

    @Test
    @DisplayName("Payment - Deve verificar getters, setters, builder e toString da entidade")
    void deveTestarEntidadePayment() {
        LocalDateTime now = LocalDateTime.now();
        Payment p = new Payment();
        p.setId(1L);
        p.setType("TRANSFER");
        p.setValue(new BigDecimal("500.00"));
        p.setDateCreation(now);
        p.setIdEnterprise(2L);
        p.setIdEnterprisePlan(3L);

        assertEquals(1L, p.getId());
        assertEquals("TRANSFER", p.getType());
        assertEquals(new BigDecimal("500.00"), p.getValue());
        assertEquals(now, p.getDateCreation());
        assertEquals(2L, p.getIdEnterprise());
        assertEquals(3L, p.getIdEnterprisePlan());
        assertTrue(p.toString().contains("TRANSFER"));

        Payment built = Payment.builder()
                .id(10L)
                .type("TED")
                .value(new BigDecimal("1000.00"))
                .idEnterprise(20L)
                .idEnterprisePlan(30L)
                .build();
        assertEquals(10L, built.getId());
        assertEquals("TED", built.getType());
        assertNotNull(built.getDateCreation());
    }
}
