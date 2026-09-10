package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.waterregistry.WaterRegistryRequestDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryResponseDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.WaterRegistryService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento de Registros de Medição de Água.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/water-registries")
@RequiredArgsConstructor
@Tag(name = "Registros de Água", description = "Endpoints para gerenciamento e apontamento de medições de hidrômetro de água")
@SecurityRequirement(name = "BearerAuth")
public class WaterRegistryController {

    private final WaterRegistryService waterRegistryService;

    /**
     * Endpoint para cadastrar uma nova medição de hidrômetro de água.
     *
     * @param request   corpo da requisição com os dados da medição
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO do registro cadastrado
     */
    @Operation(summary = "Cadastrar medição de água", description = "Registra uma nova medição de hidrômetro de água vinculada a uma Fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registro de medição de água cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou leitura final menor que inicial"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar medição")
    })
    @PostMapping
    public ResponseEntity<WaterRegistryResponseDTO> createWaterRegistry(
            @RequestBody @Valid WaterRegistryRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WaterRegistryResponseDTO response = waterRegistryService.createWaterRegistry(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar registros de medição de água acessíveis ao usuário autenticado.
     *
     * @param farmId    identificador opcional da fazenda para filtragem
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de registros de medição
     */
    @Operation(summary = "Listar registros de medição de água", description = "Lista todos os registros de medição de água da fazenda acessíveis ao usuário autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de registros de medição retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Fazenda informada para filtro não encontrada")
    })
    @GetMapping
    public ResponseEntity<List<WaterRegistryResponseDTO>> getWaterRegistries(
            @Parameter(description = "Identificador único da fazenda para filtro (opcional)", example = "1")
            @RequestParam(value = "farm_id", required = false) Long farmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(waterRegistryService.getWaterRegistriesForUser(farmId, principal));
    }

    /**
     * Endpoint para buscar os detalhes de um registro específico de medição de água pelo ID.
     *
     * @param id        identificador único do registro de medição
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os detalhes do registro
     */
    @Operation(summary = "Buscar registro de medição de água por ID", description = "Retorna os detalhes de um registro específico de medição de água.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro de medição retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Registro de medição de água não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<WaterRegistryResponseDTO> getWaterRegistryById(
            @Parameter(description = "Identificador único do registro de medição", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(waterRegistryService.getWaterRegistryById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente um registro de medição de água (leitura final do hidrômetro).
     *
     * @param id        identificador único do registro de medição
     * @param request   corpo da requisição com os campos parciais
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com o registro atualizado
     */
    @Operation(summary = "Atualizar medição de água parcialmente", description = "Atualiza a leitura final do hidrômetro ou dados pontuais da medição de água.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro de medição atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou leitura final menor que inicial"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Registro de medição de água não encontrado")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<WaterRegistryResponseDTO> updateWaterRegistry(
            @Parameter(description = "Identificador único do registro a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid WaterRegistryUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(waterRegistryService.updateWaterRegistry(id, request, principal));
    }

    /**
     * Endpoint para remover um registro de medição de água do sistema.
     *
     * @param id        identificador único do registro a ser removido
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Remover registro de medição de água", description = "Exclui um registro de medição de água do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Registro de medição removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Registro de medição de água não encontrado"),
            @ApiResponse(responseCode = "409", description = "Não é possível remover o registro de água pois existem dados vinculados")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWaterRegistry(
            @Parameter(description = "Identificador único do registro a ser removido", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        waterRegistryService.deleteWaterRegistry(id, principal);
        return ResponseEntity.noContent().build();
    }
}
