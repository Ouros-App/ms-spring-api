package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.internal.PasswordResetInternalDTO;
import com.ourosapp.springapi.dto.internal.PasswordResetInternalResponseDTO;
import com.ourosapp.springapi.service.InternalAuthService;
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
 * Controller responsável pelos endpoints internos Machine-to-Machine (M2M) do ecossistema Ouros.
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
@Tag(name = "Internal Auth", description = "Endpoints internos para comunicação Machine-to-Machine entre microsserviços")
public class InternalAuthController {

    private final InternalAuthService internalAuthService;

    /**
     * Endpoint interno para atualização de senha de usuários (produtor rural ou funcionário).
     *
     * @param dto dados para redefinição contendo tipo de conta, ID e nova senha
     * @return resposta de sucesso com mensagem explicativa
     */
    @Operation(
            summary = "Redefine a senha de um usuário internamente",
            description = "Atualiza a senha com hash BCrypt para farm_owner ou company_employee. Requer cabeçalho X-Internal-Service-Key."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Senha atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = PasswordResetInternalResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados da requisição inválidos ou regras de complexidade de senha violadas",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Acesso não autorizado devido a cabeçalho X-Internal-Service-Key ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado para o ID e tipo de conta informados",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping("/password-reset")
    public ResponseEntity<PasswordResetInternalResponseDTO> resetPassword(
            @Valid @RequestBody PasswordResetInternalDTO dto
    ) {
        internalAuthService.resetPassword(dto);
        return ResponseEntity.ok(new PasswordResetInternalResponseDTO("Senha atualizada com sucesso."));
    }
}
