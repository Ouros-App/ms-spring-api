package com.ourosapp.springapi.service;

import com.ourosapp.springapi.client.auth.AuthServiceClient;
import com.ourosapp.springapi.client.auth.dto.AuthVerifyResponseDTO;
import com.ourosapp.springapi.client.auth.exception.AuthRateLimitException;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void testLoginAdmSuccess() {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "senha123");
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                1L, "adm@ouros.com", "admin", "admin", null, null, null, null
        );

        when(authServiceClient.verifyCredentials("adm@ouros.com", "senha123", "admin"))
                .thenReturn(Optional.of(identity));
        when(jwtUtil.generateToken(1L, "adm@ouros.com", RoleConstants.ADM)).thenReturn("fake-jwt-adm");

        LoginResponseDTO response = authService.loginAdm(request);

        assertNotNull(response);
        assertEquals("fake-jwt-adm", response.token());
        assertNull(response.firstAccess());
    }

    @Test
    void testLoginAdmInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "errada");

        when(authServiceClient.verifyCredentials("adm@ouros.com", "errada", "admin"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginAdm(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
    }

    @Test
    void testLoginEmployeeSuccess() {
        LoginRequestDTO request = new LoginRequestDTO("employee@ouros.com", "senha123");
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                2L, "employee@ouros.com", "company_employee", "company_employee", "João", null, 10L, null
        );

        when(authServiceClient.verifyCredentials("employee@ouros.com", "senha123", "company_employee"))
                .thenReturn(Optional.of(identity));
        when(jwtUtil.generateToken(2L, "employee@ouros.com", RoleConstants.COMPANY_EMPLOYEE)).thenReturn("emp-token");

        LoginResponseDTO response = authService.loginEmployee(request);

        assertNotNull(response);
        assertEquals("emp-token", response.token());
        assertNull(response.firstAccess());
    }

    @Test
    void testLoginEmployeeInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("employee@ouros.com", "errada");

        when(authServiceClient.verifyCredentials("employee@ouros.com", "errada", "company_employee"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginEmployee(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
    }

    @Test
    void testLoginFarmOwnerSuccessWithFirstAccessTrue() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                3L, "farmer@ouros.com", "farm_owner", "farm_owner", "Produtor", 5L, null, true
        );

        when(authServiceClient.verifyCredentials("farmer@ouros.com", "senha123", "farm_owner"))
                .thenReturn(Optional.of(identity));
        when(jwtUtil.generateToken(3L, "farmer@ouros.com", RoleConstants.FARM_OWNER)).thenReturn("farmer-token");

        LoginResponseDTO response = authService.loginFarmOwner(request);

        assertNotNull(response);
        assertEquals("farmer-token", response.token());
        assertTrue(response.firstAccess());
    }

    @Test
    void testLoginFarmOwnerSuccessWithFirstAccessFalse() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                3L, "farmer@ouros.com", "farm_owner", "farm_owner", "Produtor", 5L, null, false
        );

        when(authServiceClient.verifyCredentials("farmer@ouros.com", "senha123", "farm_owner"))
                .thenReturn(Optional.of(identity));
        when(jwtUtil.generateToken(3L, "farmer@ouros.com", RoleConstants.FARM_OWNER)).thenReturn("farmer-token");

        LoginResponseDTO response = authService.loginFarmOwner(request);

        assertNotNull(response);
        assertEquals("farmer-token", response.token());
        assertFalse(response.firstAccess());
    }

    @Test
    void testLoginFarmOwnerInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "errada");

        when(authServiceClient.verifyCredentials("farmer@ouros.com", "errada", "farm_owner"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginFarmOwner(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
    }

    @Test
    void testLoginUnifiedAdminSuccess() {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "senha123");
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                1L, "adm@ouros.com", "admin", "admin", null, null, null, null
        );

        when(authServiceClient.verifyCredentials("adm@ouros.com", "senha123", null))
                .thenReturn(Optional.of(identity));
        when(jwtUtil.generateToken(1L, "adm@ouros.com", RoleConstants.ADM)).thenReturn("unified-token-adm");

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("unified-token-adm", response.token());
    }

    @Test
    void testLoginUnifiedCompanyEmployeeSuccess() {
        LoginRequestDTO request = new LoginRequestDTO("emp@ouros.com", "senha123");
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                2L, "emp@ouros.com", "company_employee", "company_employee", "Colaborador", null, 8L, null
        );

        when(authServiceClient.verifyCredentials("emp@ouros.com", "senha123", null))
                .thenReturn(Optional.of(identity));
        when(jwtUtil.generateToken(2L, "emp@ouros.com", RoleConstants.COMPANY_EMPLOYEE)).thenReturn("unified-token-emp");

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("unified-token-emp", response.token());
    }

    @Test
    void testLoginUnifiedFarmOwnerSuccess() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                3L, "farmer@ouros.com", "farm_owner", "farm_owner", "Produtor", 12L, null, true
        );

        when(authServiceClient.verifyCredentials("farmer@ouros.com", "senha123", null))
                .thenReturn(Optional.of(identity));
        when(jwtUtil.generateToken(3L, "farmer@ouros.com", RoleConstants.FARM_OWNER)).thenReturn("unified-token-farmer");

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("unified-token-farmer", response.token());
        assertTrue(response.firstAccess());
    }

    @Test
    void testLoginUnifiedUnknownAccountTypeThrowsException() {
        LoginRequestDTO request = new LoginRequestDTO("unknown@ouros.com", "senha123");
        AuthVerifyResponseDTO.IdentityDTO identity = new AuthVerifyResponseDTO.IdentityDTO(
                99L, "unknown@ouros.com", "unknown_role", "unknown_role", null, null, null, null
        );

        when(authServiceClient.verifyCredentials("unknown@ouros.com", "senha123", null))
                .thenReturn(Optional.of(identity));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
    }

    @Test
    void testLoginPropagatesRateLimitException() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");

        when(authServiceClient.verifyCredentials("farmer@ouros.com", "senha123", "farm_owner"))
                .thenThrow(new AuthRateLimitException("Muitas tentativas.", 45L));

        AuthRateLimitException ex = assertThrows(AuthRateLimitException.class, () -> authService.loginFarmOwner(request));
        assertEquals(45L, ex.getRetryAfterSeconds());
    }
}
