package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.common.PageResponseDTO;
import com.ourosapp.springapi.dto.common.PageQuery;
import com.ourosapp.springapi.service.PagedListingService;
import com.ourosapp.springapi.dto.lot.LotAggregatedRequestDTO;
import com.ourosapp.springapi.dto.lot.LotRequestDTO;
import com.ourosapp.springapi.dto.lot.LotResponseDTO;
import com.ourosapp.springapi.dto.lot.LotUpdateDTO;
import com.ourosapp.springapi.dto.lot.ProductionRegistryResponseDTO;
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
    private final PagedListingService pagedListingService;
    private final LotService lotService;

    @Operation(summary = "Cadastrar lote com leitura inicial", description = "Cadastra o lote e a leitura inicial do hidrômetro na mesma transação.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Lote criado"), @ApiResponse(responseCode = "400", description = "Dados inválidos"), @ApiResponse(responseCode = "401", description = "Usuário não autenticado"), @ApiResponse(responseCode = "403", description = "Acesso negado"), @ApiResponse(responseCode = "404", description = "Fazenda ou empresa não encontrada"), @ApiResponse(responseCode = "409", description = "Conflito de integridade")})
    @PostMapping("/aggregated")
    public ResponseEntity<LotResponseDTO> createAggregated(@RequestBody @Valid LotAggregatedRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LotResponseDTO response = lotService.createAggregatedLot(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/lots/{id}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Consultar resumo de produção por lote", description = "Retorna o resumo consolidado de um lote acessível ao usuário.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Resumo encontrado"), @ApiResponse(responseCode = "401", description = "Usuário não autenticado"), @ApiResponse(responseCode = "403", description = "Acesso negado"), @ApiResponse(responseCode = "404", description = "Lote não encontrado")})
    @GetMapping("/production-summaries/{id}")
    public ResponseEntity<ProductionRegistryResponseDTO> getProductionSummary(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(lotService.getProductionSummaryById(id, principal));
    }

    @Operation(summary = "Listar resumos de produção com paginação", description = "ADM acessa todas as empresas; funcionário acessa sua empresa; produtor acessa sua fazenda. Filtros e busca precedem a paginação e contagem; ID desempata a ordenação.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Página e totais filtrados"),
            @ApiResponse(responseCode = "400", description = "Página, tamanho ou ordenação inválidos"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Escopo não autorizado"),
            @ApiResponse(responseCode = "404", description = "Usuário ou fazenda não encontrados")})
    @GetMapping("/production-summaries/paged")
    public ResponseEntity<PageResponseDTO<ProductionRegistryResponseDTO>> getPaged(
            @RequestParam(name = "id_farm", required = false) Long farmId,
            @RequestParam(name = "id_enterprise", required = false) Long enterpriseId,
            @Parameter(description = "Página iniciada em zero", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho entre 1 e 100", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Busca textual sem distinguir maiúsculas") @RequestParam(defaultValue = "") String search,
            @Parameter(description = "Campo de ordenação: id, hidrometro, custo, entraram, sairam, galinhas_atuais, data_emissao, farm_name; padrão id") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Direção asc ou desc") @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pagedListingService.production(farmId, enterpriseId, new PageQuery(page, size, search, sort, direction), principal));
    }

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
     * Endpoint agregador (BFF) para listar registros consolidados de produção avícola.
     *
     * @param farmId       filtro opcional pelo ID da fazenda
     * @param enterpriseId filtro opcional pelo ID da empresa integradora
     * @param principal    dados do usuário autenticado via token JWT
     * @return resposta HTTP 200 (OK) com a lista de resumos consolidados de produção
     */
    @Operation(summary = "Listar registros consolidados de produção", description = "Retorna visão unificada de lotes, água e aves para colaboradores da empresa integradora, produtores e administradores.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de resumos de produção retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário ou fazenda de filtro não encontrado")
    })
    @GetMapping("/production-summaries")
    public ResponseEntity<List<ProductionRegistryResponseDTO>> getProductionSummaries(
            @Parameter(description = "Filtro opcional pelo ID da fazenda", example = "1")
            @RequestParam(name = "id_farm", required = false) Long farmId,
            @Parameter(description = "Filtro opcional pelo ID da empresa integradora", example = "1")
            @RequestParam(name = "id_enterprise", required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(lotService.getProductionSummaries(farmId, enterpriseId, principal));
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
