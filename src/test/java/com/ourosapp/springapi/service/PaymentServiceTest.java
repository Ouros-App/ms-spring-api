package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.payment.PaymentRequestDTO;
import com.ourosapp.springapi.dto.payment.PaymentResponseDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.EnterprisePlan;
import com.ourosapp.springapi.entity.Payment;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterprisePlanRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.PaymentRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link PaymentService}.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EnterprisePlanRepository enterprisePlanRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, "admin@ouros.com", "pass", RoleConstants.ADM,
                List.of(new SimpleGrantedAuthority("ROLE_ADM")));
        employeePrincipal = new UserPrincipal(2L, "employee@empresa.com", "pass", RoleConstants.COMPANY_EMPLOYEE,
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE")));
        farmOwnerPrincipal = new UserPrincipal(3L, "owner@fazenda.com", "pass", RoleConstants.FARM_OWNER,
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER")));
    }

    @Test
    @DisplayName("createPayment - Deve registrar pagamento com sucesso como ADM")
    void deveRegistrarPagamentoComSucessoComoAdmin() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("2990.00"), 10L, 5L);
        EnterprisePlan enterprisePlan = EnterprisePlan.builder().id(5L).idEnterprise(10L).idPlan(1L).build();
        Payment saved = Payment.builder()
                .id(1L)
                .type("PIX")
                .value(new BigDecimal("2990.00"))
                .dateCreation(LocalDateTime.now())
                .idEnterprise(10L)
                .idEnterprisePlan(5L)
                .build();

        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(enterprisePlanRepository.findById(5L)).thenReturn(Optional.of(enterprisePlan));
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponseDTO response = paymentService.createPayment(request, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("PIX", response.type());
        assertEquals(new BigDecimal("2990.00"), response.value());
        assertEquals(10L, response.idEnterprise());
        assertEquals(5L, response.idEnterprisePlan());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("createPayment - Deve registrar pagamento com sucesso como COMPANY_EMPLOYEE inferindo a empresa")
    void deveRegistrarPagamentoComSucessoComoCompanyEmployee() {
        PaymentRequestDTO request = new PaymentRequestDTO("BOLETO", new BigDecimal("1500.00"), null, 5L);
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        EnterprisePlan enterprisePlan = EnterprisePlan.builder().id(5L).idEnterprise(10L).idPlan(1L).build();
        Payment saved = Payment.builder()
                .id(1L)
                .type("BOLETO")
                .value(new BigDecimal("1500.00"))
                .dateCreation(LocalDateTime.now())
                .idEnterprise(10L)
                .idEnterprisePlan(5L)
                .build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(enterprisePlanRepository.findById(5L)).thenReturn(Optional.of(enterprisePlan));
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        PaymentResponseDTO response = paymentService.createPayment(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(10L, response.idEnterprise());
        assertEquals(5L, response.idEnterprisePlan());
    }

    @Test
    @DisplayName("createPayment - Deve lançar 401 quando não autenticado")
    void deveLancarExcecaoQuandoNaoAutenticado() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 10L, 5L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createPayment(request, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("createPayment - Deve lançar 403 quando FARM_OWNER tentar registrar pagamento")
    void deveLancarExcecaoQuandoFarmOwnerTentarRegistrarPagamento() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 10L, 5L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createPayment(request, farmOwnerPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createPayment - Deve lançar 400 quando ADM não informar idEnterprise")
    void deveLancarExcecaoQuandoAdminNaoInformarIdEnterprise() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), null, 5L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createPayment(request, adminPrincipal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("createPayment - Deve lançar 404 quando empresa não existir")
    void deveLancarExcecaoQuandoEmpresaNaoExistir() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 99L, 5L);
        when(enterpriseRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createPayment(request, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("createPayment - Deve lançar 404 quando plano contratado não existir")
    void deveLancarExcecaoQuandoPlanoContratadoNaoExistir() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 10L, 99L);
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(enterprisePlanRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createPayment(request, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("createPayment - Deve lançar 400 quando houver divergência entre a empresa e o plano contratado (FK Composta)")
    void deveLancarExcecaoQuandoHouverDivergenciaEntreEmpresaEPlanoContratado() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 10L, 5L);
        EnterprisePlan enterprisePlan = EnterprisePlan.builder().id(5L).idEnterprise(20L).idPlan(1L).build();

        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(enterprisePlanRepository.findById(5L)).thenReturn(Optional.of(enterprisePlan));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createPayment(request, adminPrincipal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("createPayment - Deve lançar 403 quando colaborador tentar pagar para outra empresa")
    void deveLancarExcecaoQuandoColaboradorTentarPagarParaOutraEmpresa() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 99L, 5L);
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createPayment(request, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createPayment - Deve lançar 409 quando ocorrer erro de integridade ao salvar")
    void deveLancarExcecaoQuandoErroIntegridadeNoSave() {
        PaymentRequestDTO request = new PaymentRequestDTO("PIX", new BigDecimal("100.00"), 10L, 5L);
        EnterprisePlan enterprisePlan = EnterprisePlan.builder().id(5L).idEnterprise(10L).idPlan(1L).build();

        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(enterprisePlanRepository.findById(5L)).thenReturn(Optional.of(enterprisePlan));
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("Erro de constraint"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createPayment(request, adminPrincipal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("getPayments - Deve listar pagamentos com filtros como ADM")
    void deveListarPagamentosComFiltrosComoAdmin() {
        Payment payment = Payment.builder().id(1L).type("PIX").value(new BigDecimal("100.00")).idEnterprise(10L).idEnterprisePlan(5L).build();
        when(paymentRepository.findByIdEnterpriseAndIdEnterprisePlan(10L, 5L)).thenReturn(List.of(payment));

        List<PaymentResponseDTO> result = paymentService.getPayments(10L, 5L, adminPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    @DisplayName("getPayments - Deve listar todos os pagamentos como ADM quando sem filtros")
    void deveListarTodosOsPagamentosComoAdmin() {
        Payment p1 = Payment.builder().id(1L).type("PIX").value(new BigDecimal("100.00")).idEnterprise(10L).idEnterprisePlan(5L).build();
        Payment p2 = Payment.builder().id(2L).type("BOLETO").value(new BigDecimal("200.00")).idEnterprise(11L).idEnterprisePlan(6L).build();
        when(paymentRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PaymentResponseDTO> result = paymentService.getPayments(null, null, adminPrincipal);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getPayments - Deve listar apenas os pagamentos da sua empresa como COMPANY_EMPLOYEE")
    void deveListarPagamentosDaSuaEmpresaComoCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        Payment payment = Payment.builder().id(1L).type("PIX").value(new BigDecimal("100.00")).idEnterprise(10L).idEnterprisePlan(5L).build();

        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(paymentRepository.findByIdEnterprise(10L)).thenReturn(List.of(payment));

        List<PaymentResponseDTO> result = paymentService.getPayments(null, null, employeePrincipal);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).idEnterprise());
    }

    @Test
    @DisplayName("getPayments - Deve lançar 403 quando COMPANY_EMPLOYEE tentar filtrar por outra empresa")
    void deveLancarExcecaoQuandoColaboradorFiltrarOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.getPayments(99L, null, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getPayments - Deve lançar 403 para perfil FARM_OWNER")
    void deveLancarExcecaoAoListarComPerfilInvalido() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.getPayments(null, null, farmOwnerPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getPaymentById - Deve buscar pagamento por ID com sucesso como ADM")
    void deveBuscarPagamentoPorIdComoAdmin() {
        Payment payment = Payment.builder().id(1L).type("PIX").value(new BigDecimal("100.00")).idEnterprise(10L).idEnterprisePlan(5L).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponseDTO response = paymentService.getPaymentById(1L, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("getPaymentById - Deve buscar pagamento por ID com sucesso como COMPANY_EMPLOYEE da mesma empresa")
    void deveBuscarPagamentoPorIdComoCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        Payment payment = Payment.builder().id(1L).type("PIX").value(new BigDecimal("100.00")).idEnterprise(10L).idEnterprisePlan(5L).build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        PaymentResponseDTO response = paymentService.getPaymentById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("getPaymentById - Deve lançar 403 quando colaborador buscar pagamento de outra empresa")
    void deveLancarExcecaoQuandoColaboradorBuscarPagamentoDeOutraEmpresa() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).idEnterprise(10L).build();
        Payment payment = Payment.builder().id(1L).type("PIX").value(new BigDecimal("100.00")).idEnterprise(99L).idEnterprisePlan(5L).build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(employee));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.getPaymentById(1L, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("getPaymentById - Deve lançar 404 quando pagamento não for encontrado")
    void deveLancarExcecaoQuandoPagamentoNaoEncontrado() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.getPaymentById(99L, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("deletePayment - Deve excluir pagamento com sucesso como ADM")
    void deveExcluirPagamentoComoAdmin() {
        Payment payment = Payment.builder().id(1L).type("PIX").value(new BigDecimal("100.00")).idEnterprise(10L).idEnterprisePlan(5L).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertDoesNotThrow(() -> paymentService.deletePayment(1L, adminPrincipal));
        verify(paymentRepository).delete(payment);
    }

    @Test
    @DisplayName("deletePayment - Deve lançar 403 quando COMPANY_EMPLOYEE tentar excluir pagamento")
    void deveLancarExcecaoQuandoNaoAdminTentarExcluirPagamento() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.deletePayment(1L, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
