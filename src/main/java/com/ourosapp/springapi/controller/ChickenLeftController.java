package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.chickenleft.ChickenLeftRequestDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftResponseDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.ChickenLeftService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento de Registros de Saída de Aves.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/chicken-left")
@RequiredArgsConstructor
@Tag(name = "Saída de Aves", description = "Endpoints para gerenciamento e registro de saída/baixa de aves")
@SecurityRequirement(name = "BearerAuth")
public class ChickenLeftController {

    private final ChickenLeftService chickenLeftService;

    /**
     * Endpoint para cadastrar uma nova saída de aves.
     *
     * @param request   corpo da requisição com os dados da saída de aves
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO do registro cadastrado
     */
    @Operation(summary = "Cadastrar saída de aves", description = "Registra uma nova saída de aves vinculada a uma Fazenda e abate a quantidade do saldo atual de aves.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registro de saída de aves cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou quantidade superior ao saldo atual de aves"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar saída de aves")
    })
    @PostMapping
    public ResponseEntity<ChickenLeftResponseDTO> createChickenLeft(
            @RequestBody @Valid ChickenLeftRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ChickenLeftResponseDTO response = chickenLeftService.createChickenLeft(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar registros de saída de aves acessíveis ao usuário autenticado.
     *
     * @param farmId    identificador opcional da fazenda para filtragem
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de registros de saída de aves
     */
    @Operation(summary = "Listar registros de saída de aves", description = "Lista todos os registros de saída de aves da fazenda acessíveis ao usuário autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de registros de saída de aves retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda informada para filtro não encontrada")
    })
    @GetMapping
    public ResponseEntity<List<ChickenLeftResponseDTO>> getChickenLefts(
            @Parameter(description = "Identificador único da fazenda para filtro (opcional)", example = "1")
            @RequestParam(value = "farm_id", required = false) Long farmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(chickenLeftService.getChickenLeftsForUser(farmId, principal));
    }

    /**
     * Endpoint para buscar os detalhes de um registro específico de saída de aves pelo ID.
     *
     * @param id        identificador único do registro de saída de aves
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os detalhes do registro
     */
    @Operation(summary = "Buscar registro de saída de aves por ID", description = "Retorna os detalhes de um registro específico de saída de aves.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro de saída de aves retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Registro de saída de aves não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ChickenLeftResponseDTO> getChickenLeftById(
            @Parameter(description = "Identificador único do registro de saída de aves", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(chickenLeftService.getChickenLeftById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente um registro de saída de aves.
     *
     * @param id        identificador único do registro a ser atualizado
     * @param request   corpo da requisição com os campos parciais
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com o registro atualizado
     */
    @Operation(summary = "Atualizar saída de aves parcialmente", description = "Atualiza a quantidade ou data de um registro de saída de aves, recalculando o saldo na fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro de saída de aves atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou novo saldo de aves insuficiente"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Registro de saída de aves não encontrado")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ChickenLeftResponseDTO> updateChickenLeft(
            @Parameter(description = "Identificador único do registro a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid ChickenLeftUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(chickenLeftService.updateChickenLeft(id, request, principal));
    }

    /**
     * Endpoint para remover um registro de saída de aves do sistema e estornar o saldo.
     *
     * @param id        identificador único do registro a ser removido
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Remover registro de saída de aves", description = "Exclui um registro de saída de aves e estorna a quantidade ao saldo de aves da fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Registro de saída de aves removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Registro de saída de aves não encontrado"),
            @ApiResponse(responseCode = "409", description = "Não é possível remover o registro de saída de aves pois existem dados vinculados")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChickenLeft(
            @Parameter(description = "Identificador único do registro a ser removido", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        chickenLeftService.deleteChickenLeft(id, principal);
        return ResponseEntity.noContent().build();
    }
}
