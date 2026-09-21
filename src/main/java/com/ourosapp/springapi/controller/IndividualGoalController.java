package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.individualgoal.IndividualGoalRequestDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalResponseDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.IndividualGoalService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento de Metas Individuais de fazendas.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/individual-goals")
@RequiredArgsConstructor
@Tag(name = "Metas Individuais", description = "Endpoints para gerenciamento de metas individuais de produtividade e sustentabilidade nas fazendas")
@SecurityRequirement(name = "BearerAuth")
public class IndividualGoalController {

    private final IndividualGoalService individualGoalService;

    /**
     * Endpoint para criar uma nova meta individual na fazenda.
     *
     * @param request   dados da meta individual (título, descrição, tipo, status, valor alvo e ID da fazenda)
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO da meta cadastrada
     */
    @Operation(summary = "Cadastrar meta individual", description = "Cria uma nova meta individual de produtividade ou sustentabilidade para a fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Meta individual cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar meta individual")
    })
    @PostMapping
    public ResponseEntity<IndividualGoalResponseDTO> createIndividualGoal(
            @RequestBody @Valid IndividualGoalRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        IndividualGoalResponseDTO response = individualGoalService.createIndividualGoal(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar todas as metas individuais das fazendas acessíveis ao usuário autenticado.
     *
     * @param farmId    filtro opcional por ID da fazenda
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de metas individuais
     */
    @Operation(summary = "Listar metas individuais", description = "Lista as metas individuais das fazendas acessíveis ao usuário autenticado, com filtro opcional por ID de fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de metas individuais retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o filtro de fazenda informado"),
            @ApiResponse(responseCode = "404", description = "Fazenda informada no filtro não encontrada")
    })
    @GetMapping
    public ResponseEntity<List<IndividualGoalResponseDTO>> getIndividualGoals(
            @Parameter(description = "ID opcional da fazenda para filtrar as metas", example = "1")
            @RequestParam(name = "farm_id", required = false) Long farmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(individualGoalService.getIndividualGoalsForUser(farmId, principal));
    }

    /**
     * Endpoint para buscar os detalhes de 1 meta individual específica pelo seu ID.
     *
     * @param id        identificador único da meta individual
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os detalhes da meta
     */
    @Operation(summary = "Buscar meta individual por ID", description = "Retorna os detalhes de 1 meta individual específica pelo seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta individual retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou meta"),
            @ApiResponse(responseCode = "404", description = "Meta individual não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<IndividualGoalResponseDTO> getIndividualGoalById(
            @Parameter(description = "Identificador único da meta individual", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(individualGoalService.getIndividualGoalById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente dados pontuais de uma meta individual (PATCH /individual-goals/{id}).
     *
     * @param id        identificador único da meta a ser atualizada
     * @param request   corpo da requisição com os campos parciais (título, descrição, status e/ou valor alvo)
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a meta individual atualizada
     */
    @Operation(summary = "Atualizar meta individual parcialmente", description = "Atualiza dados pontuais de uma meta individual (título, descrição, status ou valor alvo).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta individual atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou meta"),
            @ApiResponse(responseCode = "404", description = "Meta individual não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao atualizar meta individual")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<IndividualGoalResponseDTO> updateIndividualGoal(
            @Parameter(description = "Identificador único da meta individual a ser atualizada", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid IndividualGoalUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(individualGoalService.updateIndividualGoal(id, request, principal));
    }

    /**
     * Endpoint para remover uma meta individual do sistema.
     *
     * @param id        identificador único da meta individual a ser removida
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Excluir meta individual", description = "Exclui uma meta individual do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Meta individual removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para excluir esta meta individual"),
            @ApiResponse(responseCode = "404", description = "Meta individual não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIndividualGoal(
            @Parameter(description = "Identificador único da meta individual a ser removida", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        individualGoalService.deleteIndividualGoal(id, principal);
        return ResponseEntity.noContent().build();
    }
}
