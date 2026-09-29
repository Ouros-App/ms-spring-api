package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.payment.PaymentRequestDTO;
import com.ourosapp.springapi.dto.payment.PaymentResponseDTO;
import com.ourosapp.springapi.security.KeycloakJwtAuthenticationConverter;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.PaymentService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração Web via MockMvc para o controlador {@link PaymentController}.
 */
@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UserPrincipal mockPrincipal = new UserPrincipal(
            1L,
            "admin@ouros.com",
            "password123",
            "ADM",
            List.of(new SimpleGrantedAuthority("ROLE_ADM"))
    );

    @Test
    @DisplayName("POST /payments - Deve registrar pagamento e retornar 201 Created com cabeçalho Location")
    void testCreatePaymentSuccess() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("2990.00"), 1L, 2L);
        PaymentResponseDTO response = new PaymentResponseDTO(
                10L, "PIX", new BigDecimal("2990.00"),
                LocalDateTime.of(2026, 9, 29, 10, 30, 0),
                1L, 2L
        );

        when(paymentService.createPayment(any(PaymentRequestDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(post("/payments")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/payments/10")))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.type").value("PIX"))
                .andExpect(jsonPath("$.value").value(2990.00))
                .andExpect(jsonPath("$.id_enterprise").value(1L))
                .andExpect(jsonPath("$.id_enterprise_plan").value(2L));
    }

    @Test
    @DisplayName("POST /payments - Deve aceitar payload em formato camelCase (interoperabilidade)")
    void testCreatePaymentCamelCasePayload() throws Exception {
        String camelCasePayload = """
                {
                    "type": "PIX",
                    "value": 2990.00,
                    "idEnterprise": 1,
                    "idEnterprisePlan": 2
                }
                """;
        PaymentResponseDTO response = new PaymentResponseDTO(
                10L, "PIX", new BigDecimal("2990.00"),
                LocalDateTime.of(2026, 9, 29, 10, 30, 0),
                1L, 2L
        );

        when(paymentService.createPayment(any(PaymentRequestDTO.class), eq(mockPrincipal))).thenReturn(response);

        mockMvc.perform(post("/payments")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camelCasePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("POST /payments - Deve retornar 400 Bad Request quando payload for inválido")
    void testCreatePaymentInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "type": "",
                    "value": -100.0,
                    "id_enterprise": -1,
                    "id_enterprise_plan": null
                }
                """;

        mockMvc.perform(post("/payments")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /payments - Deve retornar 401 Unauthorized quando não autenticado")
    void testCreatePaymentUnauthorized() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 1L, 2L);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /payments - Deve retornar 403 Forbidden quando usuário sem permissão")
    void testCreatePaymentForbidden() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 1L, 2L);
        when(paymentService.createPayment(any(PaymentRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"));

        mockMvc.perform(post("/payments")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /payments - Deve retornar 404 Not Found quando empresa ou plano contratado não existir")
    void testCreatePaymentNotFound() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 1L, 2L);
        when(paymentService.createPayment(any(PaymentRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano da empresa não encontrado"));

        mockMvc.perform(post("/payments")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /payments - Deve retornar 409 Conflict quando ocorre violação de integridade")
    void testCreatePaymentConflict() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 1L, 2L);
        when(paymentService.createPayment(any(PaymentRequestDTO.class), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade"));

        mockMvc.perform(post("/payments")
                        .with(user(mockPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /payments - Deve retornar lista de pagamentos com status 200 OK")
    void testGetPaymentsSuccess() throws Exception {
        PaymentResponseDTO payment = new PaymentResponseDTO(
                1L, "PIX", new BigDecimal("2990.00"),
                LocalDateTime.of(2026, 9, 29, 10, 30, 0),
                1L, 2L
        );
        when(paymentService.getPayments(eq(1L), eq(2L), eq(mockPrincipal))).thenReturn(List.of(payment));

        mockMvc.perform(get("/payments")
                        .param("enterprise_id", "1")
                        .param("enterprise_plan_id", "2")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].type").value("PIX"))
                .andExpect(jsonPath("$[0].value").value(2990.00));
    }

    @Test
    @DisplayName("GET /payments - Deve retornar 401 Unauthorized quando não autenticado")
    void testGetPaymentsUnauthorized() throws Exception {
        mockMvc.perform(get("/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /payments/{id} - Deve retornar pagamento com status 200 OK")
    void testGetPaymentByIdSuccess() throws Exception {
        PaymentResponseDTO payment = new PaymentResponseDTO(
                1L, "PIX", new BigDecimal("2990.00"),
                LocalDateTime.of(2026, 9, 29, 10, 30, 0),
                1L, 2L
        );
        when(paymentService.getPaymentById(eq(1L), eq(mockPrincipal))).thenReturn(payment);

        mockMvc.perform(get("/payments/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("PIX"));
    }

    @Test
    @DisplayName("GET /payments/{id} - Deve retornar 404 Not Found quando pagamento não existir")
    void testGetPaymentByIdNotFound() throws Exception {
        when(paymentService.getPaymentById(eq(99L), eq(mockPrincipal)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        mockMvc.perform(get("/payments/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /payments/{id} - Deve estornar pagamento e retornar 204 No Content")
    void testDeletePaymentSuccess() throws Exception {
        doNothing().when(paymentService).deletePayment(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/payments/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /payments/{id} - Deve retornar 403 Forbidden quando perfil não for autorizado")
    void testDeletePaymentForbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas administradores podem estornar"))
                .when(paymentService).deletePayment(eq(1L), eq(mockPrincipal));

        mockMvc.perform(delete("/payments/1")
                        .with(user(mockPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /payments/{id} - Deve retornar 404 Not Found quando pagamento não existir")
    void testDeletePaymentNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado"))
                .when(paymentService).deletePayment(eq(99L), eq(mockPrincipal));

        mockMvc.perform(delete("/payments/99")
                        .with(user(mockPrincipal)))
                .andExpect(status().isNotFound());
    }
}
