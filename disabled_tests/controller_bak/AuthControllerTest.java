package com.ourosapp.springapi.controller;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void testLoginAdm() {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "senha123");
        when(authService.loginAdm(request)).thenReturn(new LoginResponseDTO("adm-token"));

        ResponseEntity<LoginResponseDTO> response = authController.loginAdm(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("adm-token", response.getBody().token());
    }

    @Test
    void testLoginEmployee() {
        LoginRequestDTO request = new LoginRequestDTO("emp@ouros.com", "senha123");
        when(authService.loginEmployee(request)).thenReturn(new LoginResponseDTO("emp-token"));

        ResponseEntity<LoginResponseDTO> response = authController.loginEmployee(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("emp-token", response.getBody().token());
    }

    @Test
    void testLoginFarmOwner() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        when(authService.loginFarmOwner(request)).thenReturn(new LoginResponseDTO("farmer-token"));

        ResponseEntity<LoginResponseDTO> response = authController.loginFarmOwner(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("farmer-token", response.getBody().token());
    }
}
