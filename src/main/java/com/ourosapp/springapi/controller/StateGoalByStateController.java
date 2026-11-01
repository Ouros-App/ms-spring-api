package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.stategoal.*;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.StateGoalByStateService;
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

@RestController
@RequestMapping("/state-goals")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Metas Estaduais", description = "Metas compartilhadas por UF e empresa")
public class StateGoalByStateController {
    private final StateGoalByStateService service;

    @GetMapping("/states")
    @Operation(summary = "Listar UFs com granjas elegíveis", description = "Retorna apenas as UFs que possuem granjas elegíveis cadastradas para a empresa no Brasil.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Lista de UFs com granjas"),
            @ApiResponse(responseCode = "400", description = "Empresa não informada ou inválida"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Escopo não autorizado"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada")})
    public ResponseEntity<java.util.List<String>> listEligibleStates(
            @RequestParam(name = "id_enterprise", required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.listEligibleStates(enterpriseId, principal));
    }

    @GetMapping("/audience")
    @Operation(summary = "Prévia de participantes da UF", description = "Conta granjas pelo estado do endereço, no Brasil, respeitando a empresa. Não vincula novas granjas automaticamente após o lançamento.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Público elegível"),
            @ApiResponse(responseCode = "400", description = "UF ou empresa inválida"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Escopo não autorizado"),
            @ApiResponse(responseCode = "404", description = "Funcionário ou empresa não encontrado")})
    public ResponseEntity<StateGoalAudienceDTO> preview(@RequestParam String state,
            @RequestParam(name = "id_enterprise", required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.preview(state, enterpriseId, principal));
    }

    @PostMapping("/by-state")
    @Operation(summary = "Criar meta para as granjas de uma UF", description = "Cria uma meta descritiva acompanhada por status, sem alvo numérico no cadastro, e todos os vínculos na mesma transação. Recalcula o público na criação e inicia cada participante como Pendente. ADM deve selecionar empresa; funcionário usa sua própria empresa.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Meta e participantes cadastrados"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou nenhuma granja elegível"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado"),
            @ApiResponse(responseCode = "403", description = "Escopo não autorizado"),
            @ApiResponse(responseCode = "404", description = "Funcionário ou empresa não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade; transação revertida")})
    public ResponseEntity<StateGoalByStateResponseDTO> create(@RequestBody @Valid StateGoalByStateRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = service.create(request, principal);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/state-goals/{id}")
                .buildAndExpand(result.id()).toUri();
        return ResponseEntity.created(location).body(result);
    }
}
