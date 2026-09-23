package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.lot.LotRequestDTO;
import com.ourosapp.springapi.dto.lot.LotResponseDTO;
import com.ourosapp.springapi.dto.lot.LotUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.LotService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento de Lotes de Aves.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/lots")
@RequiredArgsConstructor
@Tag(name = "Lotes", description = "Endpoints para gerenciamento do ciclo de vida de Lotes de Aves")
@SecurityRequirement(name = "BearerAuth")
public class LotController {

    private final LotService lotService;

    /**
     * Endpoint para iniciar e cadastrar um novo lote de aves para a fazenda.
     *
     * @param request   corpo da requisição com os dados do lote
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO do lote cadastrado
     */
    @Operation(summary = "Iniciar novo lote de aves", description = "Inicia um novo lote de aves para uma fazenda vinculada a uma Empresa Integradora.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lote de aves iniciado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou divergência entre fazenda e empresa"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Empresa ou fazenda vinculadas não encontradas"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar lote")
    })
    @PostMapping
    public ResponseEntity<LotResponseDTO> createLot(
            @RequestBody @Valid LotRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LotResponseDTO response = lotService.createLot(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar os lotes de aves acessíveis ao usuário autenticado, com filtros opcionais.
     *
     * @param idFarm       identificador opcional da fazenda para filtro
     * @param idEnterprise identificador opcional da empresa para filtro
     * @param principal    dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de lotes
     */
    @Operation(summary = "Listar lotes de aves", description = "Lista os lotes de aves de acordo com o perfil do usuário logado, permitindo filtros opcionais por fazenda ou empresa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de lotes retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário ou registro de filtro não encontrado")
    })
    @GetMapping
    public ResponseEntity<List<LotResponseDTO>> getLotsForUser(
            @Parameter(description = "Filtro opcional pelo ID da fazenda", example = "1")
            @RequestParam(value = "id_farm", required = false) Long idFarm,
            @Parameter(description = "Filtro opcional pelo ID da empresa integradora", example = "1")
            @RequestParam(value = "id_enterprise", required = false) Long idEnterprise,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lotService.getLotsForUser(idFarm, idEnterprise, principal));
    }

    /**
     * Endpoint para buscar os dados detalhados de um lote específico pelo seu ID.
     *
     * @param id        identificador único do lote
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os dados do lote
     */
    @Operation(summary = "Buscar lote por ID", description = "Retorna as informações detalhadas de um lote específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lote retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LotResponseDTO> getLotById(
            @Parameter(description = "Identificador único do lote", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lotService.getLotById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente um lote (ex.: fechamento do ciclo, aves entregues, perdas, custo).
     *
     * @param id        identificador único do lote a ser atualizado
     * @param request   corpo da requisição com os campos parciais
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com o lote atualizado
     */
    @Operation(summary = "Atualizar lote parcialmente", description = "Atualiza o lote no fechamento do ciclo produtivo (aves entregues, data de entrega, perdas, custos).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lote atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou regras de negócio violadas"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<LotResponseDTO> updateLot(
            @Parameter(description = "Identificador único do lote a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid LotUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lotService.updateLot(id, request, principal));
    }

    /**
     * Endpoint para cancelar ou excluir um lote de aves.
     *
     * @param id        identificador único do lote a ser removido
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Excluir lote de aves", description = "Cancela ou remove um lote de aves do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Lote removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Lote não encontrado"),
            @ApiResponse(responseCode = "409", description = "Não é possível remover o lote pois existem registros vinculados a ele")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLot(
            @Parameter(description = "Identificador único do lote a ser removido", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        lotService.deleteLot(id, principal);
        return ResponseEntity.noContent().build();
    }
}
