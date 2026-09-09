package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.farmowner.FarmOwnerRequestDTO;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerResponseDTO;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmOwnerService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST responsável por expor as rotas de gerenciamento de Produtores Rurais.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/farm-owners")
@RequiredArgsConstructor
@Tag(name = "Produtores Rurais", description = "Endpoints para gerenciamento e manutenção de Produtores Rurais")
@SecurityRequirement(name = "BearerAuth")
public class FarmOwnerController {

    private final FarmOwnerService farmOwnerService;

    /**
     * Endpoint para cadastrar um novo produtor rural vinculado a uma fazenda.
     *
     * @param request   corpo da requisição contendo os dados do produtor rural
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO do produtor rural criado
     */
    @Operation(summary = "Cadastrar produtor rural", description = "Cadastra um novo produtor rural vinculado a uma Fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Produtor rural cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Documento/CPF ou e-mail já cadastrados no sistema")
    })
    @PostMapping
    public ResponseEntity<FarmOwnerResponseDTO> createFarmOwner(
            @RequestBody @Valid FarmOwnerRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        FarmOwnerResponseDTO response = farmOwnerService.createFarmOwner(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para obter os dados do produtor rural atualmente autenticado via token JWT.
     *
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os dados do produtor logado
     */
    @Operation(summary = "Obter produtor rural logado", description = "Retorna os dados cadastrais do produtor rural autenticado via token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados do produtor rural retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Produtor rural não encontrado no banco de dados")
    })
    @GetMapping("/me")
    public ResponseEntity<FarmOwnerResponseDTO> getLoggedInFarmOwner(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmOwnerService.getLoggedInFarmOwner(principal));
    }

    /**
     * Endpoint para listar produtores rurais vinculados ao escopo do usuário autenticado, com filtro opcional por fazenda.
     *
     * @param farmId    filtro opcional por identificador único da fazenda
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de produtores rurais encontrados
     */
    @Operation(summary = "Listar produtores rurais", description = "Lista todos os produtores rurais associados ao perfil do usuário ou a uma fazenda específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de produtores rurais retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda ou usuário logado não encontrados")
    })
    @GetMapping
    public ResponseEntity<List<FarmOwnerResponseDTO>> getFarmOwners(
            @Parameter(description = "Identificador único da fazenda para filtragem opcional", example = "10")
            @RequestParam(name = "farmId", required = false) Long farmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmOwnerService.getFarmOwners(farmId, principal));
    }

    /**
     * Endpoint para buscar as informações de um produtor rural específico através do seu ID.
     *
     * @param id        identificador único do produtor rural
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os dados do produtor rural encontrado
     */
    @Operation(summary = "Buscar produtor rural por ID", description = "Retorna as informações de um produtor rural específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produtor rural retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Produtor rural não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FarmOwnerResponseDTO> getFarmOwnerById(
            @Parameter(description = "Identificador único do produtor rural", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmOwnerService.getFarmOwnerById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente os dados de um produtor rural existente (telefone, e-mail e/ou senha).
     *
     * @param id        identificador único do produtor rural a ser atualizado
     * @param request   corpo da requisição com os campos a serem atualizados
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) e o DTO do produtor rural atualizado
     */
    @Operation(summary = "Atualizar produtor rural parcialmente", description = "Atualiza telefone, e-mail e/ou senha de um produtor rural existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produtor rural atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Produtor rural não encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail já pertence a outro produtor rural")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<FarmOwnerResponseDTO> updateFarmOwner(
            @Parameter(description = "Identificador único do produtor rural a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid FarmOwnerUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmOwnerService.updateFarmOwner(id, request, principal));
    }

    /**
     * Endpoint para remover um produtor rural do sistema.
     *
     * @param id        identificador único do produtor rural a ser removido
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Remover produtor rural", description = "Remove um produtor rural do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Produtor rural removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Produtor rural não encontrado"),
            @ApiResponse(responseCode = "409", description = "Não é possível remover o produtor rural pois existem registros vinculados a ele")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFarmOwner(
            @Parameter(description = "Identificador único do produtor rural a ser removido", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        farmOwnerService.deleteFarmOwner(id, principal);
        return ResponseEntity.noContent().build();
    }
}
