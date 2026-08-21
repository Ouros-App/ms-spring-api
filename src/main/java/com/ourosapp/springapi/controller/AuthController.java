package com.ourosapp.springapi.controller;

import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint de autenticação para Administradores.
     *
     * @param request corpo da requisição contendo e-mail e senha
     * @return token JWT encapsulado em LoginResponseDTO
     */
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
    @PostMapping("/farm-owners/login")
    public ResponseEntity<LoginResponseDTO> loginFarmOwner(@RequestBody @Valid LoginRequestDTO request) {
        return ResponseEntity.ok(authService.loginFarmOwner(request));
    }
}
