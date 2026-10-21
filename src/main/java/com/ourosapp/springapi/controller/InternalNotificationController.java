package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.notification.InternalBatchNotificationRequestDTO;
import com.ourosapp.springapi.dto.notification.InternalBatchNotificationResponseDTO;
import com.ourosapp.springapi.service.InternalNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsável pelos endpoints internos Machine-to-Machine (M2M) de notificações.
 * Permite que microsserviços internos (ex: Engine de Audiência) enviem notificações em lote.
 */
@RestController
@RequestMapping("/internal/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Internal Notifications", description = "Endpoints internos para ingestão e disparo de notificações em lote")
public class InternalNotificationController {

    private final InternalNotificationService internalNotificationService;

    /**
     * Endpoint interno para ingestão e processamento de notificações em lote com deduplicação.
     *
     * @param request DTO com a lista de notificações a serem persistidas e despachadas
     * @return resposta de sucesso contendo o sumário de processamento do lote
     */
    @Operation(
            summary = "Ingere e dispara notificações em lote",
            description = "Processa lote de notificações de serviços internos com deduplicação estrita via event_id e envio opcional de push via FCM. Requer cabeçalho X-Internal-Service-Key."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lote de notificações processado com sucesso",
                    content = @Content(schema = @Schema(implementation = InternalBatchNotificationResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados da requisição inválidos (lista vazia, excedeu 500 itens ou campos obrigatórios ausentes)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Acesso não autorizado devido a cabeçalho X-Internal-Service-Key ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping("/batch")
    public ResponseEntity<InternalBatchNotificationResponseDTO> processBatch(
            @Valid @RequestBody InternalBatchNotificationRequestDTO request
    ) {
        InternalBatchNotificationResponseDTO response = internalNotificationService.processBatch(request);
        return ResponseEntity.ok(response);
    }
}
