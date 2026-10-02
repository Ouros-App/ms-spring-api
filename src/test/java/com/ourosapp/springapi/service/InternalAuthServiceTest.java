package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.internal.PasswordResetInternalDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalAuthServiceTest {

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InternalAuthService internalAuthService;

    @Test
    @DisplayName("Deve redefinir a senha do produtor rural com sucesso")
    void shouldResetPasswordForFarmOwnerSuccessfully() {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("farm_owner", 10L, "NovaSenha@123");
        FarmOwner farmOwner = FarmOwner.builder()
                .id(10L)
                .email("produtor@fazenda.com.br")
                .password("old_hash")
                .build();

        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded_new_hash");
        when(farmOwnerRepository.findById(10L)).thenReturn(Optional.of(farmOwner));

        internalAuthService.resetPassword(dto);

        assertEquals("encoded_new_hash", farmOwner.getPassword());
        verify(farmOwnerRepository).save(farmOwner);
        verifyNoInteractions(companyEmployeeRepository);
    }

    @Test
    @DisplayName("Deve redefinir a senha do funcionário com sucesso")
    void shouldResetPasswordForCompanyEmployeeSuccessfully() {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("company_employee", 20L, "NovaSenha@123");
        CompanyEmployee employee = CompanyEmployee.builder()
                .id(20L)
                .email("func@empresa.com.br")
                .password("old_hash")
                .build();

        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded_new_hash");
        when(companyEmployeeRepository.findById(20L)).thenReturn(Optional.of(employee));

        internalAuthService.resetPassword(dto);

        assertEquals("encoded_new_hash", employee.getPassword());
        verify(companyEmployeeRepository).save(employee);
        verifyNoInteractions(farmOwnerRepository);
    }

    @Test
    @DisplayName("Deve lançar 404 quando produtor rural não for encontrado")
    void shouldThrowNotFoundWhenFarmOwnerDoesNotExist() {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("farm_owner", 999L, "NovaSenha@123");
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded_new_hash");
        when(farmOwnerRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                internalAuthService.resetPassword(dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Produtor rural não encontrado"));
        verify(farmOwnerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 404 quando funcionário não for encontrado")
    void shouldThrowNotFoundWhenCompanyEmployeeDoesNotExist() {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("company_employee", 999L, "NovaSenha@123");
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded_new_hash");
        when(companyEmployeeRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                internalAuthService.resetPassword(dto)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Funcionário não encontrado"));
        verify(companyEmployeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar 400 quando o tipo de conta for desconhecido")
    void shouldThrowBadRequestWhenAccountTypeIsInvalid() {
        PasswordResetInternalDTO dto = new PasswordResetInternalDTO("admin", 1L, "NovaSenha@123");
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded_new_hash");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                internalAuthService.resetPassword(dto)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Tipo de conta inválido"));
    }
}
