package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e emissão de tokens JWT para Administradores, Funcionários e Fazendeiros")
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint de autenticação para Administradores.
     *
     * @param request corpo da requisição contendo e-mail e senha
     * @return token JWT encapsulado em LoginResponseDTO
     */
    @Operation(summary = "Autenticação de Administrador", description = "Valida as credenciais do Administrador e retorna um token JWT de acesso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/adms/login")
    public ResponseEntity<LoginResponseDTO> loginAdm(@RequestBody @Valid LoginRequestDTO request) {
        return ResponseEntity.ok(authService.loginAdm(request));
    }

    /**
     * Endpoint de autenticação para Funcionários da Empresa.
     *
     * @param request corpo da requisição contendo e-mail e senha
     * @return token JWT encapsulado em LoginResponseDTO
     */
    @Operation(summary = "Autenticação de Funcionário", description = "Valida as credenciais do Funcionário da Empresa e retorna um token JWT de acesso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/company-employees/login")
    public ResponseEntity<LoginResponseDTO> loginEmployee(@RequestBody @Valid LoginRequestDTO request) {
        return ResponseEntity.ok(authService.loginEmployee(request));
    }

    /**
     * Endpoint de autenticação para Proprietários Rurais / Fazendeiros.
     *
     * @param request corpo da requisição contendo e-mail e senha
     * @return token JWT encapsulado em LoginResponseDTO
     */
    @Operation(summary = "Autenticação de Fazendeiro / Proprietário", description = "Valida as credenciais do Proprietário Rural e retorna um token JWT de acesso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/farm-owners/login")
    public ResponseEntity<LoginResponseDTO> loginFarmOwner(@RequestBody @Valid LoginRequestDTO request) {
        return ResponseEntity.ok(authService.loginFarmOwner(request));
    }
}

