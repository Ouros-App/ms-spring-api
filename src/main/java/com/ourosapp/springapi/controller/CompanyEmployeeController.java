package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.CompanyEmployeeRequestDTO;
import com.ourosapp.springapi.dto.CompanyEmployeeResponseDTO;
import com.ourosapp.springapi.dto.CompanyEmployeeUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.CompanyEmployeeService;
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

/**
 * Controlador REST responsável por expor as rotas de gerenciamento de Funcionários da Empresa Integradora.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/company-employees")
@RequiredArgsConstructor
@Tag(name = "Funcionários da Empresa", description = "Endpoints para gerenciamento de funcionários da Empresa Integradora")
@SecurityRequirement(name = "BearerAuth")
public class CompanyEmployeeController {

    private final CompanyEmployeeService companyEmployeeService;

    /**
     * Endpoint para cadastrar um novo funcionário da empresa integradora.
     *
     * @param request corpo da requisição contendo os dados do funcionário
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO do funcionário criado
     */
    @Operation(summary = "Cadastrar funcionário", description = "Cadastra um novo funcionário corporativo vinculado a uma Empresa Integradora.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Funcionário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Empresa integradora vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Documento ou e-mail já cadastrados no sistema")
    })
    @PostMapping
    public ResponseEntity<CompanyEmployeeResponseDTO> createCompanyEmployee(
            @RequestBody @Valid CompanyEmployeeRequestDTO request
    ) {
        CompanyEmployeeResponseDTO response = companyEmployeeService.createCompanyEmployee(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para buscar os dados do funcionário atualmente autenticado via token JWT.
     *
     * @param principal o usuário autenticado extraído do token
     * @return resposta HTTP 200 (OK) com os dados do funcionário logado
     */
    @Operation(summary = "Obter funcionário logado", description = "Retorna os dados cadastrais do funcionário autenticado via token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados do funcionário retornados com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado no banco de dados")
    })
    @GetMapping("/me")
    public ResponseEntity<CompanyEmployeeResponseDTO> getLoggedInEmployee(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(companyEmployeeService.getLoggedInEmployee(principal));
    }

    /**
     * Endpoint para buscar as informações de um funcionário específico através do seu ID.
     *
     * @param id identificador único do funcionário
     * @return resposta HTTP 200 (OK) com os dados do funcionário encontrado
     */
    @Operation(summary = "Buscar funcionário por ID", description = "Retorna as informações de um funcionário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Funcionário retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CompanyEmployeeResponseDTO> getCompanyEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(companyEmployeeService.getCompanyEmployeeById(id));
    }

    /**
     * Endpoint para atualizar parcialmente os dados de um funcionário existente (telefone, e-mail e/ou senha).
     *
     * @param id      identificador único do funcionário a ser atualizado
     * @param request corpo da requisição com os campos a serem atualizados
     * @return resposta HTTP 200 (OK) e o DTO do funcionário atualizado
     */
    @Operation(summary = "Atualizar funcionário parcialmente", description = "Atualiza telefone, e-mail e/ou senha de um funcionário existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Funcionário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail já pertence a outro funcionário")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<CompanyEmployeeResponseDTO> updateCompanyEmployee(
            @PathVariable Long id,
            @RequestBody @Valid CompanyEmployeeUpdateDTO request
    ) {
        return ResponseEntity.ok(companyEmployeeService.updateCompanyEmployee(id, request));
    }

    /**
     * Endpoint para remover um funcionário da empresa integradora do sistema.
     *
     * @param id identificador único do funcionário a ser removido
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Remover funcionário", description = "Remove um funcionário da empresa integradora do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Funcionário removido com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompanyEmployee(@PathVariable Long id) {
        companyEmployeeService.deleteCompanyEmployee(id);
        return ResponseEntity.noContent().build();
    }
}

