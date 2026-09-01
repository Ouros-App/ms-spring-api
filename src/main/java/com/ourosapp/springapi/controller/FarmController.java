package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.farm.FarmRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Controlador REST responsável por expor as rotas de gerenciamento de Fazendas.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/farms")
@RequiredArgsConstructor
@Tag(name = "Fazendas", description = "Endpoints para gerenciamento e manutenção de Fazendas")
@SecurityRequirement(name = "BearerAuth")
public class FarmController {

    private final FarmService farmService;

    /**
     * Endpoint para cadastrar uma nova fazenda vinculada à empresa integradora e ao endereço.
     * Suporta endereço pré-existente (id_address) ou criação embutida (address).
     *
     * @param request   corpo da requisição com os dados da fazenda
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO da fazenda cadastrada
     */
    @Operation(summary = "Cadastrar fazenda", description = "Cadastra uma nova fazenda vinculada a uma Empresa Integradora e a um Endereço.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Fazenda cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Empresa ou endereço vinculados não encontrados")
    })
    @PostMapping
    public ResponseEntity<FarmResponseDTO> createFarm(
            @RequestBody @Valid FarmRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FarmResponseDTO response = farmService.createFarm(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar todas as fazendas vinculadas ao usuário logado.
     *
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de fazendas
     */
    @Operation(summary = "Listar fazendas do usuário", description = "Lista todas as fazendas associadas ao perfil do usuário autenticado no JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de fazendas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário")
    })
    @GetMapping
    public ResponseEntity<List<FarmResponseDTO>> getFarmsForUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmService.getFarmsForUser(principal));
    }

    /**
     * Endpoint para buscar os dados detalhados de uma fazenda específica através do seu ID.
     *
     * @param id        identificador único da fazenda
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os dados da fazenda
     */
    @Operation(summary = "Buscar fazenda por ID", description = "Retorna os dados detalhados de uma fazenda específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fazenda retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FarmResponseDTO> getFarmById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmService.getFarmById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente dados pontuais de uma fazenda (nome, área, região, capacidade, local).
     *
     * @param id        identificador único da fazenda a ser atualizada
     * @param request   corpo da requisição com os campos parciais
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a fazenda atualizada
     */
    @Operation(summary = "Atualizar fazenda parcialmente", description = "Atualiza dados pontuais da fazenda (nome, capacidade de aves, área, local, região).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fazenda atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<FarmResponseDTO> updateFarm(
            @PathVariable Long id,
            @RequestBody @Valid FarmUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmService.updateFarm(id, request, principal));
    }

    /**
     * Endpoint para remover uma fazenda do sistema.
     *
     * @param id        identificador único da fazenda a ser removida
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Remover fazenda", description = "Remove uma fazenda do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Fazenda removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada"),
            @ApiResponse(responseCode = "409", description = "Não é possível remover a fazenda pois existem registros vinculados a ela")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFarm(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        farmService.deleteFarm(id, principal);
        return ResponseEntity.noContent().build();
    }
}
