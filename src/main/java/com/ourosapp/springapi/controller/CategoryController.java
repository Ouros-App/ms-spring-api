package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.category.CategoryRequestDTO;
import com.ourosapp.springapi.dto.category.CategoryResponseDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.CategoryService;
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
 * Controlador REST responsável por expor as rotas de gerenciamento de Categorias de Dicas Técnicas.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categorias de Dicas", description = "Endpoints para gerenciamento de categorias temáticas de dicas operacionais")
@SecurityRequirement(name = "BearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Endpoint para criar uma nova categoria vinculada a uma dica técnica.
     *
     * @param request   dados da categoria
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO da categoria cadastrada
     */
    @Operation(summary = "Cadastrar categoria", description = "Cria uma nova categoria para classificar dicas operacionais.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoria cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Dica vinculada não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar categoria")
    })
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @RequestBody @Valid CategoryRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CategoryResponseDTO response = categoryService.createCategory(request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .build()
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar todas as categorias cadastradas no sistema.
     *
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de categorias
     */
    @Operation(summary = "Listar categorias", description = "Lista todas as categorias cadastradas no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de categorias retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(categoryService.getCategories(principal));
    }
}
