package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.common.PageQuery;
import com.ourosapp.springapi.dto.common.PageResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmPodiumRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmRankingResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmRankingSimulationResponseDTO;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import com.ourosapp.springapi.dto.farm.FarmScoreUpdateDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.FarmRankingService;
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

import java.util.List;
import java.util.Map;

/**
 * Controlador REST responsável por expor os serviços de Ranking de Fazendas utilizando Redis NoSQL e PostgreSQL.
 * Todas as rotas são protegidas por autenticação JWT (Bearer token).
 */
@RestController
@RequestMapping("/farms")
@RequiredArgsConstructor
@Tag(name = "Ranking de Granjas (NoSQL Redis)", description = "Endpoints para consulta e manutenção do ranking de fazendas e granjas")
@SecurityRequirement(name = "BearerAuth")
@Validated
public class FarmRankingController {
    @Operation(summary = "Consultar ranking paginado", description = "ADM consulta todas as empresas ou uma empresa; funcionário consulta sua empresa. Produtor não acessa o ranking completo. Busca por nome/região e ordenação por score ou farm_name; posições do ranking filtrado são preservadas entre páginas.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Página e total do ranking filtrado"),
            @ApiResponse(responseCode = "400", description = "Paginação ou ordenação inválida"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Usuário ou empresa não encontrados")})
    @GetMapping("/rankings/paged")
    public ResponseEntity<PageResponseDTO<FarmRankingResponseDTO>> getRankingsPaged(
            @RequestParam(name = "id_enterprise", required = false) Long enterpriseId,
            @RequestParam(required = false) String region,
            @Parameter(description = "Página iniciada em zero") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho entre 1 e 100") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Busca por nome ou região") @RequestParam(defaultValue = "") String search,
            @Parameter(description = "score ou farm_name") @RequestParam(defaultValue = "score") String sort,
            @Parameter(description = "asc ou desc; menor score é melhor") @RequestParam(defaultValue = "asc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmRankingService.getRankingsPaged(enterpriseId, region,
                new PageQuery(page, size, search, sort, direction), principal));
    }


    private final FarmRankingService farmRankingService;

    /**
     * Retorna a lista das granjas mais bem classificadas no ranking da empresa integradora.
     *
     * @param enterpriseId identificador opcional da empresa (apenas para perfil ADM)
     * @param limit        quantidade máxima de granjas no ranking (padrão 10)
     * @param principal    dados do usuário logado extraídos do token JWT
     * @return lista com as granjas ordenadas por pontuação
     */
    @Operation(summary = "Obter Top Granjas no Ranking", description = "Retorna a classificação ordenada das granjas utilizando o banco NoSQL Redis em memória.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking de granjas retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para este perfil de usuário"),
            @ApiResponse(responseCode = "404", description = "Empresa ou usuário não encontrados")
    })
    @GetMapping("/rankings")
    public ResponseEntity<List<FarmRankingResponseDTO>> getTopRankings(
            @Parameter(description = "Identificador da empresa integradora (opcional para ADM)", example = "1")
            @RequestParam(name = "id_enterprise", required = false) Long enterpriseId,
            @Parameter(description = "Quantidade máxima de registros a retornar", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmRankingService.getTopRankings(enterpriseId, limit, principal));
    }

    /**
     * Retorna a posição e a pontuação individual de uma granja específica no ranking.
     *
     * @param id        identificador da fazenda
     * @param principal dados do usuário logado
     * @return DTO contendo a posição ordinal e o score da fazenda
     */
    @Operation(summary = "Consultar posição da fazenda no ranking", description = "Retorna a posição ordinal (ex: 1º, 2º lugar) e a pontuação da fazenda no Redis.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Classificação da fazenda retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para consultar esta fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada")
    })
    @GetMapping("/{id}/ranking")
    public ResponseEntity<FarmRankingResponseDTO> getFarmPosition(
            @Parameter(description = "Identificador único da fazenda", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmRankingService.getFarmPosition(id, principal));
    }

    /**
     * Retorna o pódio relativo da fazenda com concorrentes imediatos (acima, atual e abaixo).
     *
     * @param id        identificador da fazenda
     * @param principal dados do usuário logado
     * @return DTO com o trio relativo de classificação
     */
    @Operation(summary = "Consultar pódio relativo da fazenda no ranking", description = "Retorna a fazenda solicitante e o trio de concorrência direta (imediatamente acima e abaixo) na sua integradora.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pódio relativo retornado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para consultar esta fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada")
    })
    @GetMapping("/{id}/ranking/podium")
    public ResponseEntity<FarmPodiumRankingResponseDTO> getFarmPodiumRanking(
            @Parameter(description = "Identificador único da fazenda", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmRankingService.getFarmPodiumRanking(id, principal));
    }

    /**
     * Simula em tempo real o ganho de posições e a nova divisão/liga de uma granja com base em um score hipotético.
     *
     * @param id        identificador da fazenda
     * @param score     score CGI hipotético a ser simulado
     * @param principal dados do usuário logado
     * @return DTO com o resultado detalhado da simulação de ranking
     */
    @Operation(summary = "Simular ganho de posições no ranking", description = "Calcula a posição e a divisão que a fazenda alcançaria no ranking da sua integradora com um score hipotético.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulação calculada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Score informado inválido"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para consultar esta fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada")
    })
    @GetMapping("/{id}/ranking/simulate")
    public ResponseEntity<FarmRankingSimulationResponseDTO> simulateFarmRanking(
            @Parameter(description = "Identificador único da fazenda", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Score CGI hipotético a ser simulado", example = "6.07")
            @RequestParam(name = "score") @Positive(message = "O score simulado deve ser positivo") Double score,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmRankingService.simulateFarmRanking(id, score, principal));
    }

    /**
     * Atualiza ou define a pontuação de ranking de uma fazenda específica no Redis.
     *
     * @param id        identificador da fazenda
     * @param request   payload com a nova pontuação
     * @param principal dados do usuário logado
     * @return dados atualizados da fazenda no ranking
     */
    @Operation(summary = "Atualizar pontuação da fazenda no ranking", description = "Grava uma nova pontuação para a fazenda na estrutura Sorted Set do Redis NoSQL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pontuação atualizada com sucesso no Redis"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para alterar pontuação desta fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada")
    })
    @PutMapping("/{id}/ranking/score")
    public ResponseEntity<FarmRankingResponseDTO> updateFarmScore(
            @Parameter(description = "Identificador único da fazenda", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid FarmScoreUpdateDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(farmRankingService.updateFarmScore(id, request, principal));
    }

    /**
     * Executa a sincronização em lote de todas as fazendas do PostgreSQL para o Redis.
     *
     * @param enterpriseId identificador opcional da empresa integradora (apenas para ADM)
     * @param principal    dados do usuário logado
     * @return mapa com o total de fazendas sincronizadas
     */
    @Operation(summary = "Sincronizar PostgreSQL com Redis NoSQL", description = "Recarrega todas as fazendas do banco relacional PostgreSQL para o Redis Sorted Set.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sincronização executada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para executar sincronização"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")
    })
    @PostMapping("/rankings/sync")
    public ResponseEntity<Map<String, Object>> syncAllFarmsToRanking(
            @Parameter(description = "Identificador da empresa integradora (opcional para ADM)", example = "1")
            @RequestParam(name = "id_enterprise", required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        long syncedCount = farmRankingService.syncAllFarmsToRanking(enterpriseId, principal);
        return ResponseEntity.ok(Map.of(
                "message", "Sincronização de ranking concluída com sucesso",
                "synced_farms", syncedCount
        ));
    }
}
