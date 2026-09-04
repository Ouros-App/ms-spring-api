package com.ourosapp.springapi.controller;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.security.JwtAuthFilter;
import com.ourosapp.springapi.security.JwtUtil;
import com.ourosapp.springapi.service.AuthService;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void testLoginAdmSuccess() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "senha123");
        when(authService.loginAdm(any(LoginRequestDTO.class))).thenReturn(new LoginResponseDTO("jwt-token-adm"));

        mockMvc.perform(post("/adms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-adm"));
    }

    @Test
    void testLoginEmployeeSuccess() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("employee@ouros.com", "senha123");
        when(authService.loginEmployee(any(LoginRequestDTO.class))).thenReturn(new LoginResponseDTO("jwt-token-employee"));

        mockMvc.perform(post("/company-employees/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-employee"));
    }

    @Test
    void testLoginFarmOwnerSuccess() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        when(authService.loginFarmOwner(any(LoginRequestDTO.class))).thenReturn(new LoginResponseDTO("jwt-token-farmer"));

        mockMvc.perform(post("/farm-owners/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-farmer"));
    }

    @Test
    void testLoginWithInvalidEmailReturnsBadRequest() throws Exception {
        LoginRequestDTO invalidRequest = new LoginRequestDTO("email-invalido", "senha123");

        mockMvc.perform(post("/adms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginWithBlankPasswordReturnsBadRequest() throws Exception {
        LoginRequestDTO invalidRequest = new LoginRequestDTO("adm@ouros.com", "");

        mockMvc.perform(post("/adms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginAdmWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "senhaIncorreta");
        when(authService.loginAdm(any(LoginRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));

        mockMvc.perform(post("/adms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLoginEmployeeWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("employee@ouros.com", "senhaIncorreta");
        when(authService.loginEmployee(any(LoginRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));

        mockMvc.perform(post("/company-employees/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLoginFarmOwnerWithInvalidCredentialsReturnsUnauthorized() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senhaIncorreta");
        when(authService.loginFarmOwner(any(LoginRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));

        mockMvc.perform(post("/farm-owners/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
