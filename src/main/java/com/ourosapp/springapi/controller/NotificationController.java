package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.notification.DeviceTokenRequestDTO;
import com.ourosapp.springapi.dto.notification.NotificationResponseDTO;
import com.ourosapp.springapi.dto.notification.UnreadCountResponseDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.DeviceTokenService;
import com.ourosapp.springapi.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST responsável por expor os endpoints de Notificações In-App e gerenciamento de tokens FCM.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Endpoints para gerenciamento de notificações In-App e tokens FCM dos dispositivos móveis")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    @Operation(summary = "Registrar token do dispositivo", description = "Cadastra ou atualiza o token FCM do aparelho do usuário autenticado para recebimento de Push Notifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token de dispositivo registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @PostMapping("/device-token")
    public ResponseEntity<Map<String, String>> registerDeviceToken(
            @Valid @RequestBody DeviceTokenRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        deviceTokenService.registerOrUpdateToken(principal, dto);
        return ResponseEntity.ok(Map.of("message", "Token de dispositivo registrado com sucesso"));
    }

    @Operation(summary = "Remover token do dispositivo", description = "Desvincula um token de aparelho associado ao usuário autenticado (ex: logout).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Token de dispositivo removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @DeleteMapping("/device-token/{token}")
    public ResponseEntity<Void> deleteDeviceToken(
            @Parameter(description = "Token FCM do dispositivo a ser desvinculado", example = "fcm_token_sample_abc123...")
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        deviceTokenService.deleteToken(principal, token);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar notificações do usuário", description = "Retorna lista paginada de notificações In-App do usuário autenticado, com filtro opcional por não lidas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de notificações retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getUserNotifications(
            @Parameter(description = "Número da página (0-indexed)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Quantidade de itens por página", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "Filtrar apenas por notificações pendentes de leitura", example = "false")
            @RequestParam(name = "unread_only", required = false) Boolean unreadOnly,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getUserNotifications(principal, unreadOnly, pageable));
    }

    @Operation(summary = "Contagem de notificações não lidas", description = "Retorna o total de notificações não lidas do usuário para exibição em badges/contadores no aplicativo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contagem de notificações não lidas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponseDTO> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal));
    }

    @Operation(summary = "Marcar notificação como lida", description = "Atualiza o status de leitura de uma notificação individual do usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificação atualizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada para o usuário")
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(
            @Parameter(description = "Identificador único da notificação", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id, principal));
    }

    @Operation(summary = "Marcar todas como lidas", description = "Marca todas as notificações pendentes do usuário autenticado como lidas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Todas as notificações foram marcadas como lidas"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markAllAsRead(principal);
        return ResponseEntity.noContent().build();
    }
}
