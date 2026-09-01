package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.EnterpriseRequestDTO;
import com.ourosapp.springapi.dto.EnterpriseResponseDTO;
import com.ourosapp.springapi.service.EnterpriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST responsável por expor as rotas de gerenciamento de Empresas Integradoras.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/enterprises")
@RequiredArgsConstructor
@Tag(name = "Empresas", description = "Endpoints para gerenciamento de Empresas Integradoras")
@SecurityRequirement(name = "BearerAuth")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    /**
     * Endpoint para cadastrar uma nova Empresa Integradora.
     *
     * @param request corpo da requisição contendo os dados da empresa
     * @return resposta HTTP com status 201 (Created), cabeçalho Location e o DTO da empresa cadastrada
     */
    @Operation(summary = "Cadastrar empresa", description = "Cadastra uma nova Empresa Integradora no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empresa cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou CNPJ inválido"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Endereço vinculado não encontrado"),
            @ApiResponse(responseCode = "409", description = "CNPJ ou e-mail já cadastrados no sistema")
    })
    @PostMapping
    public ResponseEntity<EnterpriseResponseDTO> createEnterprise(@RequestBody @Valid EnterpriseRequestDTO request) {
        EnterpriseResponseDTO response = enterpriseService.createEnterprise(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar todas as Empresas Integradoras cadastradas.
     *
     * @return resposta HTTP com status 200 (OK) e a lista de DTOs das empresas
     */
    @Operation(summary = "Listar empresas", description = "Retorna a lista de todas as Empresas Integradoras cadastradas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido")
    })
    @GetMapping
    public ResponseEntity<List<EnterpriseResponseDTO>> getAllEnterprises() {
        return ResponseEntity.ok(enterpriseService.getAllEnterprises());
    }

    /**
     * Endpoint para buscar uma empresa específica através do seu ID.
     *
     * @param id identificador único da empresa
     * @return resposta HTTP com status 200 (OK) e o DTO da empresa encontrada
     */
    @Operation(summary = "Buscar empresa por ID", description = "Retorna as informações de uma empresa específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empresa retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EnterpriseResponseDTO> getEnterpriseById(@PathVariable Long id) {
        return ResponseEntity.ok(enterpriseService.getEnterpriseById(id));
    }

    /**
     * Endpoint para substituir integralmente todas as informações cadastrais de uma empresa existente.
     *
     * @param id      identificador único da empresa a ser atualizada
     * @param request corpo da requisição com os novos dados da empresa
     * @return resposta HTTP com status 200 (OK) e o DTO da empresa atualizada
     */
    @Operation(summary = "Atualizar empresa", description = "Substitui integralmente todas as informações de uma empresa existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empresa atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou CNPJ inválido"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa ou endereço não encontrados"),
            @ApiResponse(responseCode = "409", description = "CNPJ ou e-mail já pertencem a outra empresa")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EnterpriseResponseDTO> updateEnterprise(
            @PathVariable Long id,
            @RequestBody @Valid EnterpriseRequestDTO request
    ) {
        return ResponseEntity.ok(enterpriseService.updateEnterprise(id, request));
    }
}

