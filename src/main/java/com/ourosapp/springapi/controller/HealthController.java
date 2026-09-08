package com.ourosapp.springapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST responsável por verificar a saúde e o status da aplicação (Health Check).
 */
@RestController
@Tag(name = "Health Check", description = "Monitoramento da integridade e status da aplicação")
public class HealthController {

    @Operation(summary = "Verificar integridade da API", description = "Retorna o status atual do serviço para balanceadores de carga e monitoramento.")
    @ApiResponse(responseCode = "200", description = "Aplicação em execução normal")
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}

