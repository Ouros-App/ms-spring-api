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

    @PostMapping("/adms/login")
    public ResponseEntity<LoginResponseDTO> loginAdm(@RequestBody @Valid LoginRequestDTO request) {
        return ResponseEntity.ok(authService.loginAdm(request));
    }

    @PostMapping("/company-employees/login")
    public ResponseEntity<LoginResponseDTO> loginEmployee(@RequestBody @Valid LoginRequestDTO request) {
        return ResponseEntity.ok(authService.loginEmployee(request));
    }

    @PostMapping("/farm-owners/login")
    public ResponseEntity<LoginResponseDTO> loginFarmOwner(@RequestBody @Valid LoginRequestDTO request) {
        return ResponseEntity.ok(authService.loginFarmOwner(request));
    }
}
