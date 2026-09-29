package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanRequestDTO;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanResponseDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.EnterprisePlanService;
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
 * Controlador REST para gestão de planos contratados por empresas integradoras.
 */
@RestController
@RequestMapping("/enterprise-plans")
@RequiredArgsConstructor
@Tag(name = "Planos das Empresas", description = "Endpoints para gerenciamento de assinaturas e contratação de planos por empresas integradoras")
@SecurityRequirement(name = "BearerAuth")
public class EnterprisePlanController {

    private final EnterprisePlanService enterprisePlanService;

    @Operation(summary = "Contratar plano para empresa", description = "Vincula um plano de assinatura a uma empresa integradora. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Plano contratado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou empresa"),
            @ApiResponse(responseCode = "404", description = "Empresa ou plano não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito: a empresa já possui este plano contratado")
    })
    @PostMapping
    public ResponseEntity<EnterprisePlanResponseDTO> createEnterprisePlan(
            @RequestBody @Valid EnterprisePlanRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        EnterprisePlanResponseDTO response = enterprisePlanService.createEnterprisePlan(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Listar planos de empresas", description = "Lista as contratações de planos, com filtros opcionais por empresa e plano. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de planos contratados retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário")
    })
    @GetMapping
    public ResponseEntity<List<EnterprisePlanResponseDTO>> getEnterprisePlans(
            @Parameter(description = "ID opcional da empresa para filtrar os planos", example = "1")
            @RequestParam(name = "enterprise_id", required = false) Long enterpriseId,
            @Parameter(description = "ID opcional do catálogo de plano para filtrar", example = "1")
            @RequestParam(name = "plan_id", required = false) Long planId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(enterprisePlanService.getEnterprisePlans(enterpriseId, planId, principal));
    }

    @Operation(summary = "Buscar plano contratado por ID", description = "Retorna os detalhes de um plano contratado específico. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plano contratado retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Plano contratado não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EnterprisePlanResponseDTO> getEnterprisePlanById(
            @Parameter(description = "Identificador único do vínculo do plano", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(enterprisePlanService.getEnterprisePlanById(id, principal));
    }

    @Operation(summary = "Cancelar plano de empresa", description = "Remove o vínculo de um plano de assinatura com a empresa. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Plano cancelado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Plano contratado não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito: existem pagamentos vinculados ao plano")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnterprisePlan(
            @Parameter(description = "Identificador único do vínculo do plano a ser cancelado", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        enterprisePlanService.deleteEnterprisePlan(id, principal);
        return ResponseEntity.noContent().build();
    }
}
