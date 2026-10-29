package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.farm.FarmSimulationRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST responsável pelos endpoints de persistência de cenários de simulação
 * financeira e operacional de fazendas/granjas ({@code /farms/simulations}).
 */
@RestController
@RequestMapping("/farms/simulations")
@RequiredArgsConstructor
@Tag(name = "Simulações de Fazendas", description = "Endpoints para criação, consulta, atualização e exclusão de cenários de simulação financeira e operacional")
@SecurityRequirement(name = "BearerAuth")
public class FarmSimulationController {

    private final FarmSimulationService farmSimulationService;

    /**
     * Lista os cenários de simulação acessíveis ao usuário autenticado.
     *
     * @param idFarm       filtro opcional por fazenda
     * @param idEnterprise filtro opcional por empresa integradora
     * @param principal    usuário autenticado
     * @return lista de cenários de simulação
     */
    @Operation(
            summary = "Listar cenários de simulação",
            description = "Retorna todos os cenários de simulação financeira e operacional da empresa integradora ou fazenda do usuário autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de simulações retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para consultar simulações")
    })
    @GetMapping
    public ResponseEntity<List<FarmSimulationResponseDTO>> getSimulations(
            @Parameter(description = "Filtro opcional pelo ID da fazenda", example = "1")
            @RequestParam(name = "id_farm", required = false) Long idFarm,
            @Parameter(description = "Filtro opcional pelo ID da empresa integradora (para ADM)", example = "1")
            @RequestParam(name = "id_enterprise", required = false) Long idEnterprise,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmSimulationService.getSimulations(idFarm, idEnterprise, principal));
    }

    /**
     * Cria um novo cenário de simulação financeira e operacional.
     *
     * @param dto       dados do cenário de simulação
     * @param principal usuário autenticado
     * @return cenário criado com status 201 Created
     */
    @Operation(
            summary = "Criar cenário de simulação",
            description = "Persiste um novo cenário de simulação no PostgreSQL e retorna o impacto projetado no ranking Redis."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cenário de simulação criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para criar simulação nesta fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada")
    })
    @PostMapping
    public ResponseEntity<FarmSimulationResponseDTO> createSimulation(
            @Valid @RequestBody FarmSimulationRequestDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FarmSimulationResponseDTO response = farmSimulationService.createSimulation(dto, principal);
        java.net.URI location = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Atualiza parcialmente um cenário de simulação existente.
     *
     * @param id        identificador do cenário de simulação
     * @param dto       campos a atualizar
     * @param principal usuário autenticado
     * @return cenário atualizado
     */
    @Operation(
            summary = "Atualizar cenário de simulação",
            description = "Atualiza os parâmetros de um cenário de simulação existente e recalcula o impacto no ranking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cenário de simulação atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para alterar esta simulação"),
            @ApiResponse(responseCode = "404", description = "Simulação ou fazenda não encontrada")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<FarmSimulationResponseDTO> updateSimulation(
            @Parameter(description = "Identificador único da simulação", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody FarmSimulationUpdateDTO dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmSimulationService.updateSimulation(id, dto, principal));
    }

    /**
     * Exclui um cenário de simulação existente.
     *
     * @param id        identificador do cenário de simulação
     * @param principal usuário autenticado
     * @return 204 No Content
     */
    @Operation(
            summary = "Excluir cenário de simulação",
            description = "Remove permanentemente um cenário de simulação salvo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cenário de simulação excluído com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para excluir esta simulação"),
            @ApiResponse(responseCode = "404", description = "Simulação não encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSimulation(
            @Parameter(description = "Identificador único da simulação", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        farmSimulationService.deleteSimulation(id, principal);
        return ResponseEntity.noContent().build();
    }
}
