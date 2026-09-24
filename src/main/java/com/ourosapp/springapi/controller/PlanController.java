package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.plan.PlanRequestDTO;
import com.ourosapp.springapi.dto.plan.PlanResponseDTO;
import com.ourosapp.springapi.dto.plan.PlanUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.PlanService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento e catálogo de Planos de Assinatura.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
@Tag(name = "Planos", description = "Endpoints para gerenciamento e catálogo de Planos de Assinatura")
@SecurityRequirement(name = "BearerAuth")
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "Criar novo plano", description = "Cadastra um novo plano de assinatura no catálogo. Restrito ao perfil ADM.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Plano cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou preço/duração não positivos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "409", description = "Conflito: já existe um plano cadastrado com este título")
    })
    @PostMapping
    public ResponseEntity<PlanResponseDTO> createPlan(
            @RequestBody @Valid PlanRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PlanResponseDTO response = planService.createPlan(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Listar planos disponíveis", description = "Retorna todos os planos de assinatura cadastrados no sistema. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de planos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário")
    })
    @GetMapping
    public ResponseEntity<List<PlanResponseDTO>> getAllPlans(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(planService.getAllPlans(principal));
    }

    @Operation(summary = "Buscar plano por ID", description = "Retorna os detalhes de um plano de assinatura específico. Acessível por ADM e COMPANY_EMPLOYEE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plano retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponseDTO> getPlanById(
            @Parameter(description = "Identificador único do plano", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(planService.getPlanById(id, principal));
    }

    @Operation(summary = "Atualizar plano parcialmente (PATCH)", description = "Atualiza parcialmente as informações de um plano existente. Restrito ao perfil ADM.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plano atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito: título já em uso por outro plano")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<PlanResponseDTO> patchPlan(
            @Parameter(description = "Identificador único do plano a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid PlanUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(planService.updatePlan(id, request, principal));
    }

    @Operation(summary = "Atualizar plano integralmente (PUT)", description = "Atualiza os dados de um plano existente. Restrito ao perfil ADM.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Plano atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito: título já em uso por outro plano")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PlanResponseDTO> putPlan(
            @Parameter(description = "Identificador único do plano a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid PlanUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(planService.updatePlan(id, request, principal));
    }

    @Operation(summary = "Excluir plano", description = "Remove um plano de assinatura do catálogo. Restrito ao perfil ADM.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Plano removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito: existem empresas vinculadas ao plano")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(
            @Parameter(description = "Identificador único do plano a ser removido", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        planService.deletePlan(id, principal);
        return ResponseEntity.noContent().build();
    }
}
