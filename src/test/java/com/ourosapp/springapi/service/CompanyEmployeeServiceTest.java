package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeRequestDTO;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeResponseDTO;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeUpdateDTO;
import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.repository.AdmRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private AdmRepository admRepository;

    private CompanyEmployeeService companyEmployeeService;

    private CompanyEmployee sampleEmployee;
    private CompanyEmployeeRequestDTO sampleRequest;
    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;

    /**
     * Inicializa os dados de teste antes de cada execução.
     */
    @BeforeEach
    void setUp() {
        companyEmployeeService = new CompanyEmployeeService(
                companyEmployeeRepository,
                enterpriseRepository,
                passwordEncoder,
                admRepository
        );

        sampleEmployee = CompanyEmployee.builder()
                .id(1L)
                .name("Carlos Eduardo Pereira")
                .documentNumber("12345678909")
                .email("carlos.pereira@empresa.com.br")
                .telephone("11987654321")
                .password("encoded_password_123")
                .idEnterprise(10L)
                .photoUrl("https://storage.ourosapp.com/profiles/1.jpg")
                .build();

        sampleRequest = new CompanyEmployeeRequestDTO(
                "Carlos Eduardo Pereira",
                "12345678909",
                "carlos.pereira@empresa.com.br",
                "11987654321",
                "SenhaForte@123",
                10L
        );

        adminPrincipal = new UserPrincipal(
                1L,
                "adm@empresa.com.br",
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                1L,
                "emp@empresa.com.br",
                "123",
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );
    }

    /**
     * Testa o cadastro bem-sucedido de funcionário com criptografia de senha e integridade de dados.
     */
    @Test
    @DisplayName("Deve cadastrar um novo funcionário com sucesso quando os dados forem válidos")
    void testCreateCompanyEmployeeSuccess() {
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(companyEmployeeRepository.existsByDocumentNumber("12345678909")).thenReturn(false);
        when(companyEmployeeRepository.existsByEmailIgnoreCase("carlos.pereira@empresa.com.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("encoded_password_123");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.createCompanyEmployee(sampleRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Carlos Eduardo Pereira", response.name());
        assertEquals("12345678909", response.documentNumber());
        assertEquals("carlos.pereira@empresa.com.br", response.email());
        assertEquals("11987654321", response.telephone());
        assertEquals(10L, response.idEnterprise());
        assertEquals("https://storage.ourosapp.com/profiles/1.jpg", response.photoUrl());

        verify(enterpriseRepository, times(1)).existsById(10L);
        verify(companyEmployeeRepository, times(1)).existsByDocumentNumber("12345678909");
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
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, adminPrincipal)
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
        when(companyEmployeeRepository.existsByDocumentNumber("12345678909")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, adminPrincipal)
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
        when(companyEmployeeRepository.existsByDocumentNumber("12345678909")).thenReturn(false);
        when(companyEmployeeRepository.existsByEmailIgnoreCase("carlos.pereira@empresa.com.br")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, adminPrincipal)
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
        when(companyEmployeeRepository.existsByDocumentNumber("12345678909")).thenReturn(false);
        when(companyEmployeeRepository.existsByEmailIgnoreCase("carlos.pereira@empresa.com.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("encoded_password_123");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenThrow(new DataIntegrityViolationException("Constraint violation"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, adminPrincipal)
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
                () -> companyEmployeeService.createCompanyEmployee(null, adminPrincipal)
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

        CompanyEmployeeResponseDTO response = companyEmployeeService.getCompanyEmployeeById(1L, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Carlos Eduardo Pereira", response.name());
        assertEquals("12345678909", response.documentNumber());
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
                () -> companyEmployeeService.getCompanyEmployeeById(99L, adminPrincipal)
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
        assertEquals("https://storage.ourosapp.com/profiles/1.jpg", response.photoUrl());
        verify(companyEmployeeRepository, times(1)).findById(1L);
    }

    /**
     * Testa retorno com sucesso dos dados do Administrador (ADM) logado.
     */
    @Test
    @DisplayName("Deve retornar DTO seguro de administrador quando perfil logado for ADM")
    void testGetLoggedInEmployeeAsAdmSuccess() {
        Adm adm = Adm.builder()
                .id(1L)
                .name("Admin Master")
                .email("adm@empresa.com.br")
                .photoUrl("https://storage.ourosapp.com/profiles/adm.jpg")
                .build();

        when(admRepository.findById(1L)).thenReturn(Optional.of(adm));

        CompanyEmployeeResponseDTO response = companyEmployeeService.getLoggedInEmployee(adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Admin Master", response.name());
        assertEquals("adm@empresa.com.br", response.email());
        assertEquals("https://storage.ourosapp.com/profiles/adm.jpg", response.photoUrl());
        assertNull(response.idEnterprise());
    }

    /**
     * Testa retorno sintético de ADM quando não houver registro ou nome específico no banco.
     */
    @Test
    @DisplayName("Deve retornar DTO sintético padrão para ADM quando não encontrar registro em adms")
    void testGetLoggedInEmployeeAsAdmFallbackWhenNotFoundInTable() {
        when(admRepository.findById(1L)).thenReturn(Optional.empty());
        when(admRepository.findByEmailIgnoreCase("adm@empresa.com.br")).thenReturn(Optional.empty());

        CompanyEmployeeResponseDTO response = companyEmployeeService.getLoggedInEmployee(adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Administrador da Plataforma", response.name());
        assertEquals("adm@empresa.com.br", response.email());
        assertNull(response.photoUrl());
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
     * Testa lançamento de erro 403 quando o perfil do usuário logado for FARM_OWNER na rota /me.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 403 quando o perfil não for COMPANY_EMPLOYEE nem ADM")
    void testGetLoggedInEmployeeInvalidRole() {
        UserPrincipal farmOwnerPrincipal = new UserPrincipal(
                1L,
                "produtor@fazenda.com.br",
                null,
                "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.getLoggedInEmployee(farmOwnerPrincipal)
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
     * Testa atualização completa de nome, e-mail, telefone, senha e foto de funcionário com sucesso.
     */
    @Test
    @DisplayName("Deve atualizar nome, e-mail, telefone, senha e foto do funcionário com sucesso")
    void testUpdateCompanyEmployeeFullUpdateSuccess() {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "Carlos Eduardo Atualizado",
                "carlos.novo@empresa.com.br",
                "11999998888",
                "NovaSenha@123",
                "https://storage.ourosapp.com/profiles/novo.jpg"
        );

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(companyEmployeeRepository.findByEmailIgnoreCase("carlos.novo@empresa.com.br")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("new_encoded_password");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals("Carlos Eduardo Atualizado", sampleEmployee.getName());
        assertEquals("carlos.novo@empresa.com.br", sampleEmployee.getEmail());
        assertEquals("11999998888", sampleEmployee.getTelephone());
        assertEquals("new_encoded_password", sampleEmployee.getPassword());
        assertEquals("https://storage.ourosapp.com/profiles/novo.jpg", sampleEmployee.getPhotoUrl());
        verify(companyEmployeeRepository, times(1)).save(sampleEmployee);
    }

    /**
     * Testa limpeza da foto de perfil quando photo_url for enviada em branco.
     */
    @Test
    @DisplayName("Deve limpar foto do funcionário quando photo_url for enviada como string vazia")
    void testUpdateCompanyEmployeeClearPhotoUrlSuccess() {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                null,
                null,
                null,
                null,
                ""
        );

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateDTO, employeePrincipal);

        assertNotNull(response);
        assertNull(sampleEmployee.getPhotoUrl());
        assertNull(response.photoUrl());
        verify(companyEmployeeRepository, times(1)).save(sampleEmployee);
    }

    /**
     * Testa atualização do próprio perfil de ADM quando não existir funcionário com o mesmo ID.
     */
    @Test
    @DisplayName("Deve permitir ADM atualizar próprio perfil na tabela adms quando não houver funcionário correspondente")
    void testUpdateAdmOwnProfileSuccess() {
        Adm adm = Adm.builder()
                .id(1L)
                .name("Admin Original")
                .email("adm@empresa.com.br")
                .password("old_pass")
                .build();

        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "Admin Novo Nome",
                "adm.novo@empresa.com.br",
                "11988887777",
                "NovaSenha@123",
                "https://storage.ourosapp.com/profiles/adm_new.jpg"
        );

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.empty());
        when(admRepository.findById(1L)).thenReturn(Optional.of(adm));
        when(admRepository.findByEmailIgnoreCase("adm.novo@empresa.com.br")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded_adm_pass");
        when(admRepository.save(any(Adm.class))).thenReturn(adm);

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateDTO, adminPrincipal);

        assertNotNull(response);
        assertEquals("Admin Novo Nome", response.name());
        assertEquals("adm.novo@empresa.com.br", response.email());
        assertEquals("11988887777", response.telephone());
        assertEquals("https://storage.ourosapp.com/profiles/adm_new.jpg", response.photoUrl());
        verify(admRepository).save(adm);
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

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateDTO, adminPrincipal);

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

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateDTO, adminPrincipal);

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
                () -> companyEmployeeService.updateCompanyEmployee(1L, updateDTO, adminPrincipal)
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
                () -> companyEmployeeService.updateCompanyEmployee(99L, updateDTO, adminPrincipal)
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
                () -> companyEmployeeService.updateCompanyEmployee(1L, null, adminPrincipal)
        );

        assertEquals("O payload da requisição não pode ser nulo", exception.getMessage());
    }

    /**
     * Testa exclusão bem-sucedida de funcionário por ID.
     */
    @Test
    @DisplayName("Deve excluir funcionário com sucesso quando o ID existir")
    void testDeleteCompanyEmployeeSuccess() {
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        assertDoesNotThrow(() -> companyEmployeeService.deleteCompanyEmployee(1L, adminPrincipal));

        verify(companyEmployeeRepository, times(1)).findById(1L);
        verify(companyEmployeeRepository, times(1)).deleteById(1L);
    }

    /**
     * Testa lançamento de erro 404 ao tentar excluir funcionário inexistente.
     */
    @Test
    @DisplayName("Deve lançar ResponseStatusException 404 ao tentar excluir funcionário inexistente")
    void testDeleteCompanyEmployeeNotFound() {
        when(companyEmployeeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.deleteCompanyEmployee(99L, adminPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(companyEmployeeRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deve permitir COMPANY_EMPLOYEE cadastrar funcionário na mesma empresa")
    void testCreateCompanyEmployee_AsCompanyEmployee_SameEnterprise_Success() {
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(companyEmployeeRepository.existsByDocumentNumber(sampleRequest.documentNumber())).thenReturn(false);
        when(companyEmployeeRepository.existsByEmailIgnoreCase(sampleRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(sampleRequest.password())).thenReturn("encoded_password_123");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.createCompanyEmployee(sampleRequest, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve proibir COMPANY_EMPLOYEE de cadastrar funcionário em outra empresa")
    void testCreateCompanyEmployee_AsCompanyEmployee_DifferentEnterprise_Forbidden() {
        CompanyEmployee employeeInDifferentEnterprise = CompanyEmployee.builder().idEnterprise(99L).build();
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(employeeInDifferentEnterprise));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> companyEmployeeService.createCompanyEmployee(sampleRequest, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Não é permitido cadastrar funcionário em outra empresa"));
    }

    @Test
    @DisplayName("Deve proibir cadastro com perfil desconhecido")
    void testCreateCompanyEmployee_AsUnknownRole_Forbidden() {
        UserPrincipal unknownPrincipal = new UserPrincipal(1L, "uk@empresa.com.br", "123", "UNKNOWN", List.of());
        
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> companyEmployeeService.createCompanyEmployee(sampleRequest, unknownPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @DisplayName("Deve permitir COMPANY_EMPLOYEE acessar funcionário da mesma empresa")
    void testGetCompanyEmployeeById_AsCompanyEmployee_SameEnterprise_Success() {
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        CompanyEmployeeResponseDTO response = companyEmployeeService.getCompanyEmployeeById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Deve proibir COMPANY_EMPLOYEE de acessar funcionário de outra empresa")
    void testGetCompanyEmployeeById_AsCompanyEmployee_DifferentEnterprise_Forbidden() {
        CompanyEmployee targetEmployee = CompanyEmployee.builder().id(2L).idEnterprise(99L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee));
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> companyEmployeeService.getCompanyEmployeeById(2L, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @DisplayName("Deve proibir acesso com perfil desconhecido")
    void testGetCompanyEmployeeById_AsUnknownRole_Forbidden() {
        UserPrincipal unknownPrincipal = new UserPrincipal(1L, "uk@empresa.com.br", "123", "UNKNOWN", List.of());
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> companyEmployeeService.getCompanyEmployeeById(1L, unknownPrincipal));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @DisplayName("Deve permitir COMPANY_EMPLOYEE atualizar próprios dados")
    void testUpdateCompanyEmployee_AsCompanyEmployee_OwnData_Success() {
        CompanyEmployeeUpdateDTO updateRequest = new CompanyEmployeeUpdateDTO("novo@empresa.com", "11999999999", "NovaSenha@123");
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(companyEmployeeRepository.findByEmailIgnoreCase("novo@empresa.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded_new");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.updateCompanyEmployee(1L, updateRequest, employeePrincipal);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Deve proibir COMPANY_EMPLOYEE de atualizar outro funcionário")
    void testUpdateCompanyEmployee_AsCompanyEmployee_OtherEmployee_Forbidden() {
        CompanyEmployeeUpdateDTO updateRequest = new CompanyEmployeeUpdateDTO("novo@empresa.com", "11999999999", "NovaSenha@123");
        
        CompanyEmployee targetEmployee = CompanyEmployee.builder().id(2L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> companyEmployeeService.updateCompanyEmployee(2L, updateRequest, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @DisplayName("Deve permitir COMPANY_EMPLOYEE excluir próprios dados")
    void testDeleteCompanyEmployee_AsCompanyEmployee_OwnData_Success() {
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));

        assertDoesNotThrow(() -> companyEmployeeService.deleteCompanyEmployee(1L, employeePrincipal));
        verify(companyEmployeeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Deve proibir COMPANY_EMPLOYEE de excluir outro funcionário")
    void testDeleteCompanyEmployee_AsCompanyEmployee_OtherEmployee_Forbidden() {
        CompanyEmployee targetEmployee = CompanyEmployee.builder().id(2L).build();
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> companyEmployeeService.deleteCompanyEmployee(2L, employeePrincipal));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException 409 em conflito de integridade ao atualizar")
    void testUpdateCompanyEmployee_DataIntegrityViolation() {
        CompanyEmployeeUpdateDTO updateRequest = new CompanyEmployeeUpdateDTO("novo@empresa.com", "11999999999", "NovaSenha@123");
        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.of(sampleEmployee));
        when(companyEmployeeRepository.findByEmailIgnoreCase("novo@empresa.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded_new");
        
        when(companyEmployeeRepository.save(any())).thenThrow(new DataIntegrityViolationException("Conflict"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> companyEmployeeService.updateCompanyEmployee(1L, updateRequest, adminPrincipal));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Conflito de integridade de dados ao atualizar funcionário"));
    }

    @Test
    @DisplayName("Deve rejeitar criação quando principal autenticado não possuir ID")
    void testCreateCompanyEmployeeAuthenticatedPrincipalWithoutIdUnauthorized() {
        UserPrincipal principalWithoutId = new UserPrincipal(
                null,
                "adm@empresa.com.br",
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> companyEmployeeService.createCompanyEmployee(sampleRequest, principalWithoutId)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(companyEmployeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir cadastro público de funcionário sem principal autenticado")
    void testCreateCompanyEmployeeAnonymousSuccess() {
        when(enterpriseRepository.existsById(10L)).thenReturn(true);
        when(companyEmployeeRepository.existsByDocumentNumber("12345678909")).thenReturn(false);
        when(companyEmployeeRepository.existsByEmailIgnoreCase("carlos.pereira@empresa.com.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("encoded_password_123");
        when(companyEmployeeRepository.save(any(CompanyEmployee.class))).thenReturn(sampleEmployee);

        CompanyEmployeeResponseDTO response = companyEmployeeService.createCompanyEmployee(sampleRequest, null);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(companyEmployeeRepository).save(any(CompanyEmployee.class));
    }
@Test
    @DisplayName("Deve lancar ResponseStatusException 409 quando adm tentar atualizar email para um ja existente")
    void testUpdateAdmOwnProfile_EmailConflict() {
        CompanyEmployeeUpdateDTO updateRequest = new CompanyEmployeeUpdateDTO("Novo Nome", "existente@ouros.com", "11988887777", "NovaSenha@123", "https://photo.jpg");
        Adm currentAdm = Adm.builder().id(1L).email("adm@empresa.com.br").name("Admin").build();
        Adm otherAdm = Adm.builder().id(2L).email("existente@ouros.com").name("Outro Admin").build();

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.empty());
        when(admRepository.findById(1L)).thenReturn(Optional.of(currentAdm));
        when(admRepository.findByEmailIgnoreCase("existente@ouros.com")).thenReturn(Optional.of(otherAdm));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> companyEmployeeService.updateCompanyEmployee(1L, updateRequest, adminPrincipal));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName("Deve lancar ResponseStatusException 404 quando adm nao for encontrado no repositorio")
    void testUpdateAdmOwnProfile_NotFound() {
        CompanyEmployeeUpdateDTO updateRequest = new CompanyEmployeeUpdateDTO("Novo Nome", null, null, null, null);

        when(companyEmployeeRepository.findById(1L)).thenReturn(Optional.empty());
        when(admRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> companyEmployeeService.updateCompanyEmployee(1L, updateRequest, adminPrincipal));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("Deve buscar adm por email quando nao encontrado por ID em getLoggedInEmployee")
    void testGetLoggedInEmployee_AdmLookupByEmailFallback() {
        UserPrincipal adminWithFallback = new UserPrincipal(
                999L,
                "adm@empresa.com.br",
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
        Adm adm = Adm.builder().id(99L).email("adm@empresa.com.br").name("Admin Por Email").photoUrl("https://photo.jpg").build();
        when(admRepository.findById(999L)).thenReturn(Optional.empty());
        when(admRepository.findByEmailIgnoreCase("adm@empresa.com.br")).thenReturn(Optional.of(adm));

        CompanyEmployeeResponseDTO response = companyEmployeeService.getLoggedInEmployee(adminWithFallback);

        assertNotNull(response);
        assertEquals("Admin Por Email", response.name());
        assertEquals("https://photo.jpg", response.photoUrl());
    }
}
