package com.ourosapp.springapi.service;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para a camada de serviço {@link CompanyEmployeeService}.
 */
@ExtendWith(MockitoExtension.class)
class CompanyEmployeeServiceTest {

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CompanyEmployeeService companyEmployeeService;

    private CompanyEmployee sampleEmployee;
    private CompanyEmployeeRequestDTO sampleRequest;

    /**
     * Inicializa os dados de teste antes de cada execução.
     */
    @BeforeEach
    void setUp() {
        sampleEmployee = CompanyEmployee.builder()
                .id(1L)
                .name("Carlos Eduardo Pereira")
                .documentNumber("12345678901")
                .email("carlos.pereira@empresa.com.br")
                .telephone("11987654321")
                .password("encoded_password_123")
                .idEnterprise(10L)
                .build();

        sampleRequest = new CompanyEmployeeRequestDTO(
                "Carlos Eduardo Pereira",
                "12345678901",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                "SenhaForte@123",
                10L
        );
    }

    /**
     * Testa o cadastro bem-sucedido de funcionário com criptografia de senha e integridade de dados.
     */
    @Test
    @DisplayName("Deve cadastrar um novo funcionário com sucesso quando os dados forem válidos")
    void testCreateCompanyEmployeeSuccess() {
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(companyEmployeeRepository.existsByDocumentNumber("12345678901")).thenReturn(false);
        when(companyEmployeeRepository.existsByEmailIgnoreCase("carlos.pereira@empresa.com.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("encoded_password_123");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.createCompanyEmployee(sampleRequest, any(UserPrincipal.class));

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Carlos Eduardo Pereira", response.name());
        assertEquals("12345678901", response.documentNumber());
        assertEquals("carlos.pereira@empresa.com.br", response.email());
        assertEquals("11987654321", response.telephone());
        assertEquals(10L, response.idEnterprise());

        verify(enterpriseRepository, times(1)).existsById(10L);
        verify(companyEmployeeRepository, times(1)).existsByDocumentNumber("12345678901");
        verify(companyEmployeeRepository, times(1)).existsByEmailIgnoreCase("carlos.pereira@empresa.com.br");
        verify(passwordEncoder, times(1)).encode("SenhaForte@123");
        verify(companyEmployeeRepository, times(1)).save(any(CompanyEmployee.class));
    }

    /**
     * Testa lançamento de erro 404 ao cadastrar com ID de empresa inexistente.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar cadastrar funcionário com empresa inexistente")
    void testCreateCompanyEmployeeEnterpriseNotFound() {
        when(enterpriseRepository.existsById(10L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, any(UserPrincipal.class))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("10"));
        verify(companyEmployeeRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 409 ao cadastrar com documento/CPF já existente.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 409 ao tentar cadastrar funcionário com documento duplicado")
    void testCreateCompanyEmployeeDuplicateDocumentNumber() {
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(companyEmployeeRepository.existsByDocumentNumber("12345678901")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, any(UserPrincipal.class))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("documento"));
        verify(companyEmployeeRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 409 ao cadastrar com e-mail já existente.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 409 ao tentar cadastrar funcionário com e-mail duplicado")
    void testCreateCompanyEmployeeDuplicateEmail() {
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(companyEmployeeRepository.existsByDocumentNumber("12345678901")).thenReturn(false);
        when(companyEmployeeRepository.existsByEmailIgnoreCase("carlos.pereira@empresa.com.br")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, any(UserPrincipal.class))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("e-mail"));
        verify(companyEmployeeRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 409 quando o repositório lança DataIntegrityViolationException.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 409 quando ocorrer DataIntegrityViolationException no cadastro")
    void testCreateCompanyEmployeeDataIntegrityViolation() {
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(companyEmployeeRepository.existsByDocumentNumber("12345678901")).thenReturn(false);
        when(companyEmployeeRepository.existsByEmailIgnoreCase("carlos.pereira@empresa.com.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("encoded_password_123");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenThrow(new DataIntegrityViolationException("Constraint violation"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, any(UserPrincipal.class))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("integridade"));
    }

    /**
     * Testa lançamento de NullPointerException ao passar payload nulo no cadastro.
     */
    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar cadastrar com payload nulo")
    void testCreateCompanyEmployeeNullPayload() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> companyEmployeeService.createCompanyEmployee(null, any(UserPrincipal.class))
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
        verify(companyEmployeeRepository, never()).save(any());
    }

    /**
     * Testa busca bem-sucedida de funcionário por ID.
     */
    @Test
    @DisplayName("Deve buscar funcionário por ID com sucesso")
    void testGetCompanyEmployeeByIdSuccess() {
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        CompanyEmployeeResponseDTO response = companyEmployeeService.getCompanyEmployeeById(1L, any(UserPrincipal.class));

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Carlos Eduardo Pereira", response.name());
        assertEquals("12345678901", response.documentNumber());
        assertEquals("carlos.pereira@empresa.com.br", response.email());
        verify(companyEmployeeRepository, times(1)).findById(1L);
    }

    /**
     * Testa lançamento de erro 404 ao buscar funcionário por ID inexistente.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao buscar funcionário por ID inexistente")
    void testGetCompanyEmployeeByIdNotFound() {
        when(companyEmployeeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.getCompanyEmployeeById(99L, any(UserPrincipal.class))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("99"));
        verify(companyEmployeeRepository, times(1)).findById(99L);
    }

    /**
     * Testa retorno com sucesso dos dados do funcionário atualmente logado.
     */
    @Test
    @DisplayName("Deve retornar dados do funcionário logado com sucesso")
    void testGetLoggedInEmployeeSuccess() {
        UserPrincipal principal = new UserPrincipal(
                1L,
                "carlos.pereira@empresa.com.br",
                null,
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        CompanyEmployeeResponseDTO response = companyEmployeeService.getLoggedInEmployee(principal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Carlos Eduardo Pereira", response.name());
        assertEquals("carlos.pereira@empresa.com.br", response.email());
        verify(companyEmployeeRepository, times(1)).findById(1L);
    }

    /**
     * Testa lançamento de erro 401 quando o UserPrincipal for nulo na rota /me.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 401 quando o principal for nulo")
    void testGetLoggedInEmployeeNullPrincipal() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.getLoggedInEmployee(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    /**
     * Testa lançamento de erro 403 quando o perfil do usuário logado não for COMPANY_EMPLOYEE.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 403 quando o perfil não for COMPANY_EMPLOYEE")
    void testGetLoggedInEmployeeInvalidRole() {
        UserPrincipal admPrincipal = new UserPrincipal(
                1L,
                "adm@empresa.com.br",
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.getLoggedInEmployee(admPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("restrito"));
    }

    /**
     * Testa lançamento de erro 404 quando o ID do funcionário logado não for encontrado no banco.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 quando funcionário autenticado não for achado no banco")
    void testGetLoggedInEmployeeNotFoundInDb() {
        UserPrincipal principal = new UserPrincipal(
                99L,
                "carlos.pereira@empresa.com.br",
                null,
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        when(companyEmployeeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.getLoggedInEmployee(principal)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("99"));
    }

    /**
     * Testa atualização completa de e-mail, telefone e senha de funcionário com sucesso.
     */
    @Test
    @DisplayName("Deve atualizar e-mail, telefone e senha do funcionário com sucesso")
    void testUpdateCompanyEmployeeFullUpdateSuccess() {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "carlos.novo@empresa.com.br",
                "11999998888",
                "NovaSenha@123"
        );

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(companyEmployeeRepository.findByEmailIgnoreCase("carlos.novo@empresa.com.br")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("new_encoded_password");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateDTO, any(UserPrincipal.class));

        assertNotNull(response);
        assertEquals("carlos.novo@empresa.com.br", sampleEmployee.getEmail());
        assertEquals("11999998888", sampleEmployee.getTelephone());
        assertEquals("new_encoded_password", sampleEmployee.getPassword());
        verify(companyEmployeeRepository, times(1)).save(sampleEmployee);
    }

    /**
     * Testa atualização de funcionário mantendo o mesmo e-mail já pertencente a ele próprio.
     */
    @Test
    @DisplayName("Deve atualizar funcionário mantendo o mesmo e-mail do próprio funcionário")
    void testUpdateCompanyEmployeeKeepingSameEmail() {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "carlos.pereira@empresa.com.br",
                "11999998888",
                null
        );

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(companyEmployeeRepository.findByEmailIgnoreCase("carlos.pereira@empresa.com.br")).thenReturn(Optional.of(sampleEmployee));
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateDTO, any(UserPrincipal.class));

        assertNotNull(response);
        assertEquals("11999998888", sampleEmployee.getTelephone());
        verify(passwordEncoder, never()).encode(any());
        verify(companyEmployeeRepository, times(1)).save(sampleEmployee);
    }

    /**
     * Testa atualização parcial sem campos modificados, retornando o funcionário existente sem chamar save.
     */
    @Test
    @DisplayName("Deve retornar dados do funcionário sem chamar save quando payload não tiver alterações")
    void testUpdateCompanyEmployeeWithoutUpdates() {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(null, null, null);

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateDTO, any(UserPrincipal.class));

        assertNotNull(response);
        assertEquals(sampleEmployee.getId(), response.id());
        assertEquals(sampleEmployee.getName(), response.name());
        verify(companyEmployeeRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 409 ao tentar atualizar para e-mail pertencente a outro funcionário.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 409 ao tentar atualizar para e-mail de outro funcionário")
    void testUpdateCompanyEmployeeDuplicateEmailOtherUser() {
        CompanyEmployee otherEmployee = CompanyEmployee.builder()
                .id(2L)
                .email("outro@empresa.com.br")
                .build();

        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "outro@empresa.com.br",
                null,
                null
        );

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(companyEmployeeRepository.findByEmailIgnoreCase("outro@empresa.com.br")).thenReturn(Optional.of(otherEmployee));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.updateCompanyEmployee(1L, updateDTO, any(UserPrincipal.class))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("e-mail"));
        verify(companyEmployeeRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 404 ao tentar atualizar funcionário inexistente.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar atualizar funcionário inexistente")
    void testUpdateCompanyEmployeeNotFound() {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "novo@empresa.com.br",
                null,
                null
        );

        when(companyEmployeeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.updateCompanyEmployee(99L, updateDTO, any(UserPrincipal.class))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(companyEmployeeRepository, never()).save(any());
    }

    /**
     * Testa lançamento de NullPointerException ao tentar atualizar com payload nulo.
     */
    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar atualizar com payload nulo")
    void testUpdateCompanyEmployeeNullPayload() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> companyEmployeeService.updateCompanyEmployee(1L, null, any(UserPrincipal.class))
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
    }

    /**
     * Testa exclusão bem-sucedida de funcionário por ID.
     */
    @Test
    @DisplayName("Deve excluir funcionário com sucesso quando o ID existir")
    void testDeleteCompanyEmployeeSuccess() {
        when(companyEmployeeRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> companyEmployeeService.deleteCompanyEmployee(1L, any(UserPrincipal.class)));

        verify(companyEmployeeRepository, times(1)).existsById(1L);
        verify(companyEmployeeRepository, times(1)).deleteById(1L);
    }

    /**
     * Testa lançamento de erro 404 ao tentar excluir funcionário inexistente.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar excluir funcionário inexistente")
    void testDeleteCompanyEmployeeNotFound() {
        when(companyEmployeeRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.deleteCompanyEmployee(99L, any(UserPrincipal.class))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(companyEmployeeRepository, never()).deleteById(any());
    }
}
