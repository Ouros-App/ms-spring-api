package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.review.ReviewRequestDTO;
import com.ourosapp.springapi.dto.review.ReviewResponseDTO;
import com.ourosapp.springapi.dto.review.ReviewUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.ReviewService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST responsável por expor os endpoints de gerenciamento de Avaliações (Reviews) de Dicas Técnicas.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Avaliações", description = "Endpoints para gerenciamento de avaliações (reviews) de dicas técnicas")
@SecurityRequirement(name = "BearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Endpoint para cadastrar uma nova avaliação (comentário e nota de 0 a 5) para uma dica técnica.
     *
     * @param tipId     identificador único da dica técnica a ser avaliada
     * @param request   corpo da requisição contendo os dados da avaliação
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 201 (Created), cabeçalho Location e o DTO da avaliação criada
     */
    @Operation(summary = "Cadastrar avaliação de dica", description = "Cadastra uma nova avaliação (comentário e nota de 0 a 5) para uma dica técnica específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Avaliação cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou nota fora do intervalo [0, 5]"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil do usuário na fazenda vinculada à dica"),
            @ApiResponse(responseCode = "404", description = "Dica técnica não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao cadastrar avaliação")
    })
    @PostMapping("/tips/{tipId}/reviews")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Parameter(description = "Identificador único da dica técnica", example = "10")
            @PathVariable Long tipId,
            @RequestBody @Valid ReviewRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ReviewResponseDTO response = reviewService.createReview(tipId, request, principal);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/reviews/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Endpoint para listar todas as avaliações de uma dica técnica específica.
     *
     * @param tipId     identificador único da dica técnica
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de avaliações
     */
    @Operation(summary = "Listar avaliações de uma dica", description = "Retorna todas as avaliações e comentários registrados para uma dica técnica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de avaliações retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil do usuário"),
            @ApiResponse(responseCode = "404", description = "Dica técnica não encontrada")
    })
    @GetMapping("/tips/{tipId}/reviews")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByTipId(
            @Parameter(description = "Identificador único da dica técnica", example = "10")
            @PathVariable Long tipId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByTipId(tipId, principal));
    }

    /**
     * Endpoint para buscar os dados de uma avaliação específica por ID.
     *
     * @param id        identificador único da avaliação
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os detalhes da avaliação
     */
    @Operation(summary = "Buscar avaliação por ID", description = "Retorna os detalhes de uma avaliação específica de dica técnica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avaliação retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil do usuário"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @GetMapping("/reviews/{id}")
    public ResponseEntity<ReviewResponseDTO> getReviewById(
            @Parameter(description = "Identificador único da avaliação", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reviewService.getReviewById(id, principal));
    }

    /**
     * Endpoint para atualizar parcialmente o comentário ou nota de uma avaliação.
     *
     * @param id        identificador único da avaliação a ser atualizada
     * @param request   corpo da requisição com os campos a serem atualizados
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com os dados da avaliação atualizada
     */
    @Operation(summary = "Atualizar avaliação parcialmente", description = "Atualiza o comentário ou nota de uma avaliação previamente cadastrada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avaliação atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou nota fora do intervalo [0, 5]"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil do usuário"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade de dados ao atualizar avaliação")
    })
    @PatchMapping("/reviews/{id}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @Parameter(description = "Identificador único da avaliação a ser atualizada", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid ReviewUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reviewService.updateReview(id, request, principal));
    }

    /**
     * Endpoint para remover uma avaliação do sistema.
     *
     * @param id        identificador único da avaliação a ser removida
     * @param principal dados do usuário autenticado via token JWT
     * @return resposta HTTP 204 (No Content) sem corpo
     */
    @Operation(summary = "Remover avaliação", description = "Exclui uma avaliação do sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Avaliação removida com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil do usuário"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada"),
            @ApiResponse(responseCode = "409", description = "Não é possível remover a avaliação pois existem dependências vinculadas")
    })
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "Identificador único da avaliação a ser removida", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reviewService.deleteReview(id, principal);
        return ResponseEntity.noContent().build();
    }
}
