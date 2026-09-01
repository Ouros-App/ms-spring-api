package com.ourosapp.springapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que fornece informações gerais sobre a aplicação.
 * Endpoint público que retorna mensagem de boas-vindas e status da API.
 */
@RestController
@Tag(name = "Geral", description = "Endpoints informativos e utilitários da aplicação")
public class HomeController {

    /**
     * Endpoint raiz da API que retorna mensagem de boas-vindas.
     *
     * @return mapa contendo a mensagem informativa da aplicação
     */
    @Operation(summary = "Mensagem de boas-vindas", description = "Retorna a mensagem informativa de inicialização da API.")
    @ApiResponse(responseCode = "200", description = "Informações retornadas com sucesso")
    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
            "message",
            "Spring REST API com Gradle pronta para evoluir."
        );
    }
}

