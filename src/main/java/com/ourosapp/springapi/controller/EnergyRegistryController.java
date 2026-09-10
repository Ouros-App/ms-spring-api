package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryRequestDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryResponseDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.EnergyRegistryService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento e apontamentos de consumo de Energia Elétrica.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/energy-registries")
@RequiredArgsConstructor
@Tag(name = "Registros de Energia", description = "Endpoints para apontamentos periódicos de consumo de energia elétrica nas fazendas")
@SecurityRequirement(name = "BearerAuth")
public class EnergyRegistryController {

    private final EnergyRegistryService energyRegistryService;

    /**
     * Endpoint para registrar um novo consumo de energia na fazenda.
     *
     * @param request   dados do registro (data, consumo e ID da fazenda)
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO do registro cadastrado
     */
    @Operation(summary = "Cadastrar registro de energia", description = "Registra um novo apontamento de consumo de energia elétrica em uma fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registro de energia cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar registro de energia")
    })
    @PostMapping
    public ResponseEntity<EnergyRegistryResponseDTO> createEnergyRegistry(
            @RequestBody @Valid EnergyRegistryRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        EnergyRegistryResponseDTO response = energyRegistryService.createEnergyRegistry(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar todos os registros de consumo de energia das fazendas acessíveis ao usuário autenticado.
     *
     * @param farmId    filtro opcional por ID da fazenda
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de registros de energia
     */
    @Operation(summary = "Listar registros de consumo de energia", description = "Lista os registros de consumo de energia das fazendas acessíveis ao usuário autenticado, com filtro opcional por ID de fazenda.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de registros de energia retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o filtro de fazenda informado"),
            @ApiResponse(responseCode = "404", description = "Fazenda informada no filtro não encontrada")
    })
    @GetMapping
    public ResponseEntity<List<EnergyRegistryResponseDTO>> getEnergyRegistries(
            @Parameter(description = "ID opcional da fazenda para filtrar os registros", example = "1")
            @RequestParam(name = "farm_id", required = false) Long farmId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(energyRegistryService.getEnergyRegistriesForUser(farmId, principal));
    }

    /**
     * Endpoint para buscar os detalhes de 1 registro de consumo de energia específico pelo seu ID.
     *
     * @param id        identificador único do registro de energia
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os detalhes do registro
     */
    @Operation(summary = "Buscar registro de energia por ID", description = "Retorna os detalhes de 1 registro de consumo de energia específico pelo seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro de energia retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou registro"),
            @ApiResponse(responseCode = "404", description = "Registro de energia não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EnergyRegistryResponseDTO> getEnergyRegistryById(
            @Parameter(description = "Identificador único do registro de energia", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(energyRegistryService.getEnergyRegistryById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente dados pontuais de um registro de consumo de energia (PATCH /energy-registries/{id}).
     *
     * @param id        identificador único do registro de energia a ser atualizado
     * @param request   corpo da requisição com os campos parciais (data e/ou consumo)
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com o registro atualizado
     */
    @Operation(summary = "Atualizar registro de energia parcialmente", description = "Atualiza dados pontuais de um registro de consumo de energia (data e/ou consumo de energia).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro de energia atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil ou registro"),
            @ApiResponse(responseCode = "404", description = "Registro de energia não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao atualizar registro de energia")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<EnergyRegistryResponseDTO> updateEnergyRegistry(
            @Parameter(description = "Identificador único do registro de energia a ser atualizado", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid EnergyRegistryUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(energyRegistryService.updateEnergyRegistry(id, request, principal));
    }

    /**
     * Endpoint para remover um registro de consumo de energia do sistema.
     *
     * @param id        identificador único do registro a ser removido
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Excluir registro de energia", description = "Exclui um registro de consumo de energia do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Registro de energia removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para excluir este registro de energia"),
            @ApiResponse(responseCode = "404", description = "Registro de energia não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnergyRegistry(
            @Parameter(description = "Identificador único do registro de energia a ser removido", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        energyRegistryService.deleteEnergyRegistry(id, principal);
        return ResponseEntity.noContent().build();
    }
}
