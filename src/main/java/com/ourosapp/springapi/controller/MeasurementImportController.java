package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.measurementimport.MeasurementImportRequestDTO;
import com.ourosapp.springapi.dto.measurementimport.MeasurementImportResponseDTO;
import com.ourosapp.springapi.security.UserPrincipal;
import com.ourosapp.springapi.service.MeasurementImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/measurement-imports")
@RequiredArgsConstructor
@Tag(name = "Importação de medições", description = "Cadastro em lote de registros de água e energia")
@SecurityRequirement(name = "BearerAuth")
public class MeasurementImportController {
    private final MeasurementImportService measurementImportService;

    @PostMapping
    @Operation(summary = "Importar registros de medição", description = "Cadastra registros de água e energia em uma única operação transacional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registros cadastrados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Lote ou registro inválido"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado a uma fazenda"),
            @ApiResponse(responseCode = "404", description = "Fazenda não encontrada")
    })
    public ResponseEntity<MeasurementImportResponseDTO> createImport(
            @RequestBody @Valid MeasurementImportRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(201).body(measurementImportService.createImport(request, principal));
    }
}
