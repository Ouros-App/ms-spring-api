package com.ourosapp.springapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.client.auth.exception.AuthRateLimitException;
import com.ourosapp.springapi.config.GlobalExceptionHandler;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
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
                .andExpect(jsonPath("$.token").value("jwt-token-adm"))
                .andExpect(jsonPath("$.first_access").doesNotExist());
    }

    @Test
    void testLoginEmployeeSuccess() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("employee@ouros.com", "senha123");
        when(authService.loginEmployee(any(LoginRequestDTO.class))).thenReturn(new LoginResponseDTO("jwt-token-employee"));

        mockMvc.perform(post("/company-employees/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-employee"))
                .andExpect(jsonPath("$.first_access").doesNotExist());
    }

    @Test
    void testLoginFarmOwnerSuccess() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        when(authService.loginFarmOwner(any(LoginRequestDTO.class))).thenReturn(new LoginResponseDTO("jwt-token-farmer", true));

        mockMvc.perform(post("/farm-owners/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-farmer"))
                .andExpect(jsonPath("$.first_access").value(true));
    }

    @Test
    void testLoginUnifiedSuccess() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(new LoginResponseDTO("jwt-token-unified", false));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-unified"))
                .andExpect(jsonPath("$.first_access").value(false));
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

    @Test
    void testLoginRateLimitExceededReturns429WithRetryAfter() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        when(authService.loginFarmOwner(any(LoginRequestDTO.class)))
                .thenThrow(new AuthRateLimitException("Muitas tentativas de login. Tente novamente mais tarde.", 60L));

        mockMvc.perform(post("/farm-owners/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(jsonPath("$.detail").value("Muitas tentativas de login. Tente novamente mais tarde."));
    }

    @Test
    void testLoginAuthServiceUnavailableReturns503() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "senha123");
        when(authService.loginAdm(any(LoginRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação temporariamente indisponível."));

        mockMvc.perform(post("/adms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Serviço de autenticação temporariamente indisponível."));
    }
}
