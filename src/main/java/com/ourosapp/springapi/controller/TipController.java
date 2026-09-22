package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.tip.TipRequestDTO;
import com.ourosapp.springapi.dto.tip.TipResponseDTO;
import com.ourosapp.springapi.dto.tip.TipUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.TipService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento de Dicas Técnicas Avícolas.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/tips")
@RequiredArgsConstructor
@Tag(name = "Dicas Técnicas", description = "Endpoints para gerenciamento e consulta de recomendações operacionais e dicas avícolas")
@SecurityRequirement(name = "BearerAuth")
public class TipController {

    private final TipService tipService;

    /**
     * Endpoint para criar uma nova dica técnica vinculada a uma fazenda.
     *
     * @param request   dados da dica técnica
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO da dica cadastrada
     */
    @Operation(summary = "Cadastrar dica técnica", description = "Cria uma nova dica operacional vinculada a uma fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dica técnica cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário ou fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda ou categoria vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar dica")
    })
    @PostMapping
    public ResponseEntity<TipResponseDTO> createTip(
            @RequestBody @Valid TipRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        TipResponseDTO response = tipService.createTip(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar dicas técnicas acessíveis ao usuário autenticado, com filtro opcional por fazenda.
     *
     * @param farmId    filtro opcional por ID da fazenda
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de dicas técnicas
     */
    @Operation(summary = "Listar dicas técnicas", description = "Lista todas as dicas operacionais acessíveis ao usuário, exibindo média de notas e total de avaliações.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de dicas técnicas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda informada no filtro não encontrada")
    })
    @GetMapping
    public ResponseEntity<List<TipResponseDTO>> getTips(
            @Parameter(description = "Identificador único da fazenda para filtro (opcional)", example = "1")
            @RequestParam(name = "farm_id", required = false) Long farmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(tipService.getTipsForUser(farmId, principal));
    }

    /**
     * Endpoint para buscar os detalhes de uma dica técnica específica pelo seu ID.
     *
     * @param id        identificador único da dica
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os detalhes da dica
     */
    @Operation(summary = "Buscar dica técnica por ID", description = "Mostra as informações detalhadas de uma dica técnica específica pelo seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dica técnica retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Dica técnica não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TipResponseDTO> getTipById(
            @Parameter(description = "Identificador único da dica técnica", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(tipService.getTipById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente uma dica técnica (texto e/ou categorias associadas).
     *
     * @param id        identificador único da dica
     * @param request   dados de atualização
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a dica atualizada
     */
    @Operation(summary = "Atualizar dica técnica parcialmente", description = "Edita o texto ou as categorias associadas a uma dica técnica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dica técnica atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Dica técnica ou categoria informada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao atualizar dica")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<TipResponseDTO> updateTip(
            @Parameter(description = "Identificador único da dica a ser atualizada", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid TipUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(tipService.updateTip(id, request, principal));
    }

    /**
     * Endpoint para excluir uma dica técnica do sistema.
     *
     * @param id        identificador único da dica a ser removida
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Excluir dica técnica", description = "Exclui uma dica técnica cadastrada no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Dica técnica removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Dica técnica não encontrada"),
            @ApiResponse(responseCode = "409", description = "Não é possível remover a dica técnica pois existem dados vinculados")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTip(
            @Parameter(description = "Identificador único da dica a ser removida", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        tipService.deleteTip(id, principal);
        return ResponseEntity.noContent().build();
    }
}
