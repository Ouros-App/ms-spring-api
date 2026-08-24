package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AdmRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AdmRepository admRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("dummy-encoded-hash");
    }

    @Test
    void testLoginAdmSuccess() {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "senha123");
        Adm adm = Adm.builder().id(1L).email("adm@ouros.com").password("hashedSenha").build();

        when(admRepository.findByEmail("adm@ouros.com")).thenReturn(Optional.of(adm));
        when(passwordEncoder.matches("senha123", "hashedSenha")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "adm@ouros.com", "ADM")).thenReturn("fake-jwt-token");

        LoginResponseDTO response = authService.loginAdm(request);

        assertNotNull(response);
        assertEquals("fake-jwt-token", response.token());
    }

    @Test
    void testLoginAdmUserNotFound() {
        LoginRequestDTO request = new LoginRequestDTO("notfound@ouros.com", "senha123");
        when(admRepository.findByEmail("notfound@ouros.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginAdm(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
        verify(passwordEncoder).matches(eq("senha123"), eq("dummy-encoded-hash"));
    }

    @Test
    void testLoginAdmWrongPassword() {
        LoginRequestDTO request = new LoginRequestDTO("adm@ouros.com", "errada");
        Adm adm = Adm.builder().id(1L).email("adm@ouros.com").password("hashedSenha").build();

        when(admRepository.findByEmail("adm@ouros.com")).thenReturn(Optional.of(adm));
        when(passwordEncoder.matches("errada", "hashedSenha")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginAdm(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
    }

    @Test
    void testLoginEmployeeSuccess() {
        LoginRequestDTO request = new LoginRequestDTO("emp@ouros.com", "senha123");
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).email("emp@ouros.com").password("hashedSenha").build();

        when(companyEmployeeRepository.findByEmail("emp@ouros.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("senha123", "hashedSenha")).thenReturn(true);
        when(jwtUtil.generateToken(2L, "emp@ouros.com", "COMPANY_EMPLOYEE")).thenReturn("emp-token");

        LoginResponseDTO response = authService.loginEmployee(request);

        assertNotNull(response);
        assertEquals("emp-token", response.token());
    }

    @Test
    void testLoginEmployeeNotFound() {
        LoginRequestDTO request = new LoginRequestDTO("emp@ouros.com", "senha123");
        when(companyEmployeeRepository.findByEmail("emp@ouros.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginEmployee(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
        verify(passwordEncoder).matches(eq("senha123"), eq("dummy-encoded-hash"));
    }

    @Test
    void testLoginEmployeeWrongPassword() {
        LoginRequestDTO request = new LoginRequestDTO("emp@ouros.com", "errada");
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).email("emp@ouros.com").password("hashedSenha").build();

        when(companyEmployeeRepository.findByEmail("emp@ouros.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("errada", "hashedSenha")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginEmployee(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
    }

    @Test
    void testLoginFarmOwnerSuccess() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        FarmOwner owner = FarmOwner.builder().id(3L).email("farmer@ouros.com").password("hashedSenha").build();

        when(farmOwnerRepository.findByEmail("farmer@ouros.com")).thenReturn(Optional.of(owner));
        when(passwordEncoder.matches("senha123", "hashedSenha")).thenReturn(true);
        when(jwtUtil.generateToken(3L, "farmer@ouros.com", "FARM_OWNER")).thenReturn("farmer-token");

        LoginResponseDTO response = authService.loginFarmOwner(request);

        assertNotNull(response);
        assertEquals("farmer-token", response.token());
    }

    @Test
    void testLoginFarmOwnerNotFound() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "senha123");
        when(farmOwnerRepository.findByEmail("farmer@ouros.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginFarmOwner(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
        verify(passwordEncoder).matches(eq("senha123"), eq("dummy-encoded-hash"));
    }

    @Test
    void testLoginFarmOwnerWrongPassword() {
        LoginRequestDTO request = new LoginRequestDTO("farmer@ouros.com", "errada");
        FarmOwner owner = FarmOwner.builder().id(3L).email("farmer@ouros.com").password("hashedSenha").build();

        when(farmOwnerRepository.findByEmail("farmer@ouros.com")).thenReturn(Optional.of(owner));
        when(passwordEncoder.matches("errada", "hashedSenha")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.loginFarmOwner(request));
        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Credenciais inválidas.", ex.getReason());
    }
}
