package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.stategoal.RegionGoalRequestDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalRequestDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.StateGoalService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento de Metas Estaduais,
 * incluindo associações com múltiplas fazendas (farm_goals) e regiões (regions_goals).
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/state-goals")
@RequiredArgsConstructor
@Tag(name = "Metas Estaduais", description = "Endpoints para gerenciamento de metas estaduais de produtividade e sustentabilidade nas fazendas")
@SecurityRequirement(name = "BearerAuth")
public class StateGoalController {

    private final StateGoalService stateGoalService;

    @Operation(summary = "Cadastrar meta estadual", description = "Cria uma nova meta estadual para a fazenda com base na região.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Meta estadual cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar meta estadual")
    })
    @PostMapping
    public ResponseEntity<StateGoalResponseDTO> createStateGoal(
            @RequestBody @Valid StateGoalRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        StateGoalResponseDTO response = stateGoalService.createStateGoal(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Listar metas estaduais", description = "Lista as metas estaduais acessíveis ao usuário autenticado, com filtros opcionais por ID de fazenda e região.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de metas estaduais retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o filtro informado"),
            @ApiResponse(responseCode = "404", description = "Fazenda informada no filtro não encontrada")
    })
    @GetMapping
    public ResponseEntity<List<StateGoalResponseDTO>> getStateGoals(
            @Parameter(description = "ID opcional da fazenda para filtrar as metas", example = "1")
            @RequestParam(name = "farm_id", required = false) Long farmId,
            @Parameter(description = "Região opcional para filtrar as metas", example = "Sudeste")
            @RequestParam(name = "region", required = false) String region,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(stateGoalService.getStateGoalsForUser(farmId, region, principal));
    }

    @Operation(summary = "Buscar meta estadual por ID", description = "Retorna os detalhes de 1 meta estadual específica pelo seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta estadual retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou meta"),
            @ApiResponse(responseCode = "404", description = "Meta estadual não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StateGoalResponseDTO> getStateGoalById(
            @Parameter(description = "Identificador único da meta estadual", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(stateGoalService.getStateGoalById(id, principal));
    }

    @Operation(summary = "Atualizar meta estadual parcialmente", description = "Atualiza dados pontuais de uma meta estadual (status, data de término ou valor alvo).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta estadual atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou meta"),
            @ApiResponse(responseCode = "404", description = "Meta estadual não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao atualizar meta estadual")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<StateGoalResponseDTO> updateStateGoal(
            @Parameter(description = "Identificador único da meta estadual a ser atualizada", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid StateGoalUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(stateGoalService.updateStateGoal(id, request, principal));
    }

    @Operation(summary = "Excluir meta estadual", description = "Exclui uma meta estadual do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Meta estadual removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para excluir esta meta estadual"),
            @ApiResponse(responseCode = "404", description = "Meta estadual não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStateGoal(
            @Parameter(description = "Identificador único da meta estadual a ser removida", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        stateGoalService.deleteStateGoal(id, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vincular fazenda à meta estadual", description = "Associa uma fazenda participante a uma meta estadual existente (tabela farm_goals).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fazenda vinculada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para vincular fazenda"),
            @ApiResponse(responseCode = "404", description = "Meta estadual ou fazenda não encontrada")
    })
    @PostMapping("/{id}/farms/{farmId}")
    public ResponseEntity<Void> addFarmToStateGoal(
            @PathVariable Long id,
            @PathVariable Long farmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        stateGoalService.addFarmToStateGoal(id, farmId, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desvincular fazenda da meta estadual", description = "Remove o vínculo de uma fazenda com a meta estadual (tabela farm_goals).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fazenda desvinculada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou tentativa de desvincular a fazenda principal"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Meta estadual ou fazenda não encontrada")
    })
    @DeleteMapping("/{id}/farms/{farmId}")
    public ResponseEntity<Void> removeFarmFromStateGoal(
            @PathVariable Long id,
            @PathVariable Long farmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        stateGoalService.removeFarmFromStateGoal(id, farmId, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar fazendas da meta estadual", description = "Retorna todas as fazendas vinculadas à meta estadual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de fazendas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Meta estadual não encontrada")
    })
    @GetMapping("/{id}/farms")
    public ResponseEntity<List<FarmResponseDTO>> getFarmsByStateGoalId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(stateGoalService.getFarmsByStateGoalId(id, principal));
    }

    @Operation(summary = "Adicionar região à meta estadual", description = "Vincula uma região à meta estadual (tabelas regions_goals e state_goal_regions).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Região adicionada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Meta estadual não encontrada")
    })
    @PostMapping("/{id}/regions")
    public ResponseEntity<Void> addRegionToStateGoal(
            @PathVariable Long id,
            @RequestBody @Valid RegionGoalRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        stateGoalService.addRegionToStateGoal(id, request, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remover região da meta estadual", description = "Desvincula uma região da meta estadual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Região removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Meta estadual não encontrada")
    })
    @DeleteMapping("/{id}/regions/{region}")
    public ResponseEntity<Void> removeRegionFromStateGoal(
            @PathVariable Long id,
            @PathVariable String region,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        stateGoalService.removeRegionFromStateGoal(id, region, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar regiões da meta estadual", description = "Retorna todas as regiões vinculadas à meta estadual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de regiões retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Meta estadual não encontrada")
    })
    @GetMapping("/{id}/regions")
    public ResponseEntity<List<String>> getRegionsByStateGoalId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(stateGoalService.getRegionsByStateGoalId(id, principal));
    }
}
