package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.payment.PaymentRequestDTO;
import com.ourosapp.springapi.dto.payment.PaymentResponseDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para gestão e registro de pagamentos de planos por empresas integradoras.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Endpoints para registro e consulta de pagamentos de planos de empresas integradoras")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Registrar pagamento de plano", description = "Registra uma nova transação financeira de pagamento para um plano contratado. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pagamento registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou divergência de empresa"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou empresa"),
            @ApiResponse(responseCode = "404", description = "Empresa ou plano contratado não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade ao registrar pagamento")
    })
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @RequestBody @Valid PaymentRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PaymentResponseDTO response = paymentService.createPayment(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Listar pagamentos", description = "Consulta o histórico de pagamentos com filtros opcionais por empresa e plano contratado. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pagamentos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário")
    })
    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getPayments(
            @Parameter(description = "ID opcional da empresa para filtrar os pagamentos", example = "1")
            @RequestParam(name = "enterprise_id", required = false) Long enterpriseId,
            @Parameter(description = "ID opcional do plano contratado para filtrar", example = "1")
            @RequestParam(name = "enterprise_plan_id", required = false) Long enterprisePlanId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(paymentService.getPayments(enterpriseId, enterprisePlanId, principal));
    }

    @Operation(summary = "Buscar pagamento por ID", description = "Retorna os detalhes de um pagamento específico. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagamento retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @Parameter(description = "Identificador único do pagamento", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(paymentService.getPaymentById(id, principal));
    }

    @Operation(summary = "Estornar / Remover pagamento", description = "Remove um registro de pagamento do sistema. Restrito exclusivamente ao perfil ADM.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pagamento removido/estornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: apenas administradores podem estornar pagamentos"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(
            @Parameter(description = "Identificador único do pagamento a ser estornado", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        paymentService.deletePayment(id, principal);
        return ResponseEntity.noContent().build();
    }
}
