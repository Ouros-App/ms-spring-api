package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.farmowner.FarmOwnerRequestDTO;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerResponseDTO;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para a camada de serviço {@link FarmOwnerService}.
 */
@ExtendWith(MockitoExtension.class)
class FarmOwnerServiceTest {

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private FarmOwnerService farmOwnerService;

    private Farm sampleFarm;
    private FarmOwner sampleFarmOwner;
    private FarmOwnerRequestDTO sampleRequest;
    private UserPrincipal admPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;
    private CompanyEmployee sampleEmployee;

    /**
     * Configura o cenário de dados de teste antes de cada execução.
     */
    @BeforeEach
    void setUp() {
        sampleFarm = Farm.builder()
                .id(10L)
                .name("Fazenda Boa Vista")
                .areaProperty(new BigDecimal("150.00"))
                .region("Sudeste")
                .poultryCapacity(50000)
                .place("Gleba 1")
                .idAddress(1L)
                .idEnterprise(100L)
                .build();

        sampleFarmOwner = FarmOwner.builder()
                .id(1L)
                .name("Sebastião da Silva")
                .documentNumber("12345678909")
                .email("sebastiao.silva@fazenda.com.br")
                .telephone("11987654321")
                .password("encoded_password_123")
                .idFarm(10L)
                .build();

        sampleRequest = new FarmOwnerRequestDTO(
                "Sebastião da Silva",
                "12345678909",
                "sebastiao.silva@fazenda.com.br",
                "11987654321",
                "SenhaForte@123",
                10L
        );

        admPrincipal = new UserPrincipal(
                999L,
                "adm@ouros.com",
                null,
                ADM,
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                200L,
                "emp@empresa.com",
                null,
                COMPANY_EMPLOYEE,
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        farmOwnerPrincipal = new UserPrincipal(
                1L,
                "sebastiao.silva@fazenda.com.br",
                null,
                FARM_OWNER,
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );

        sampleEmployee = CompanyEmployee.builder()
                .id(200L)
                .name("Funcionário Integrador")
                .documentNumber("98765432100")
                .email("emp@empresa.com")
                .telephone("11999998888")
                .password("encoded_pwd")
                .idEnterprise(100L)
                .build();
    }

    /**
     * Testa cadastro de produtor rural com sucesso por um usuário administrador.
     */
    @Test
    @DisplayName("Deve cadastrar produtor rural com sucesso por ADM")
    void testCreateFarmOwnerByAdmSuccess() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(farmOwnerRepository.existsByDocumentNumber("12345678909")).thenReturn(false);
        when(farmOwnerRepository.existsByEmailIgnoreCase("sebastiao.silva@fazenda.com.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("encoded_password_123");
        when(farmOwnerRepository.save(any(FarmOwner.class))).thenReturn(sampleFarmOwner);

        FarmOwnerResponseDTO response = farmOwnerService.createFarmOwner(sampleRequest, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Sebastião da Silva", response.name());
        assertEquals("12345678909", response.documentNumber());
        assertEquals("sebastiao.silva@fazenda.com.br", response.email());
        assertEquals("11987654321", response.telephone());
        assertEquals(10L, response.idFarm());

        verify(farmOwnerRepository, times(1)).save(any(FarmOwner.class));
    }

    /**
     * Testa cadastro de produtor rural com sucesso por um funcionário da mesma integradora da fazenda.
     */
    @Test
    @DisplayName("Deve cadastrar produtor rural com sucesso por Funcionário da mesma empresa integradora")
    void testCreateFarmOwnerByCompanyEmployeeSuccess() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(200L)).thenReturn(Optional.of(sampleEmployee));
        when(farmOwnerRepository.existsByDocumentNumber("12345678909")).thenReturn(false);
        when(farmOwnerRepository.existsByEmailIgnoreCase("sebastiao.silva@fazenda.com.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("encoded_password_123");
        when(farmOwnerRepository.save(any(FarmOwner.class))).thenReturn(sampleFarmOwner);

        FarmOwnerResponseDTO response = farmOwnerService.createFarmOwner(sampleRequest, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(companyEmployeeRepository, times(1)).findById(200L);
        verify(farmOwnerRepository, times(1)).save(any(FarmOwner.class));
    }

    /**
     * Testa lançamento de erro 403 quando funcionário corporativo tenta cadastrar em fazenda de outra integradora.
     */
    @Test
    @DisplayName("Deve lançar 403 Forbidden se Funcionário tentar cadastrar produtor em fazenda de outra empresa")
    void testCreateFarmOwnerByCompanyEmployeeOtherEnterpriseForbidden() {
        sampleFarm.setIdEnterprise(9999L);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(companyEmployeeRepository.findById(200L)).thenReturn(Optional.of(sampleEmployee));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.createFarmOwner(sampleRequest, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("outra empresa"));
        verify(farmOwnerRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 403 quando um perfil sem autorização tenta cadastrar produtor rural.
     */
    @Test
    @DisplayName("Deve lançar 403 Forbidden se outro perfil sem permissão tentar cadastrar produtor rural")
    void testCreateFarmOwnerForbiddenProfile() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.createFarmOwner(sampleRequest, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(farmOwnerRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 404 quando o ID da fazenda informada não existe.
     */
    @Test
    @DisplayName("Deve lançar 404 Not Found se a fazenda informada não existir")
    void testCreateFarmOwnerFarmNotFound() {
        when(farmRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.createFarmOwner(sampleRequest, admPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("10"));
        verify(farmOwnerRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 409 quando o CPF informado já pertence a outro produtor cadastrado.
     */
    @Test
    @DisplayName("Deve lançar 409 Conflict se o CPF já estiver cadastrado")
    void testCreateFarmOwnerDocumentConflict() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(farmOwnerRepository.existsByDocumentNumber("12345678909")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.createFarmOwner(sampleRequest, admPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("documento"));
        verify(farmOwnerRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 409 quando o e-mail informado já pertence a outro produtor cadastrado.
     */
    @Test
    @DisplayName("Deve lançar 409 Conflict se o e-mail já estiver cadastrado")
    void testCreateFarmOwnerEmailConflict() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(farmOwnerRepository.existsByDocumentNumber("12345678909")).thenReturn(false);
        when(farmOwnerRepository.existsByEmailIgnoreCase("sebastiao.silva@fazenda.com.br")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.createFarmOwner(sampleRequest, admPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("e-mail"));
        verify(farmOwnerRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 409 quando o banco lança DataIntegrityViolationException no salvamento.
     */
    @Test
    @DisplayName("Deve lançar 409 Conflict em DataIntegrityViolationException")
    void testCreateFarmOwnerDataIntegrityViolation() {
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(farmOwnerRepository.existsByDocumentNumber("12345678909")).thenReturn(false);
        when(farmOwnerRepository.existsByEmailIgnoreCase("sebastiao.silva@fazenda.com.br")).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte@123")).thenReturn("encoded_password_123");
        when(farmOwnerRepository.save(any(FarmOwner.class))).thenThrow(new DataIntegrityViolationException("Constraint"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.createFarmOwner(sampleRequest, admPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    /**
     * Testa lançamento de NullPointerException quando o payload de cadastro for nulo.
     */
    @Test
    @DisplayName("Deve lançar NullPointerException quando payload for nulo")
    void testCreateFarmOwnerNullPayload() {
        assertThrows(NullPointerException.class, () -> farmOwnerService.createFarmOwner(null, admPrincipal));
    }

    /**
     * Testa retorno com sucesso dos dados do produtor rural atualmente autenticado via JWT.
     */
    @Test
    @DisplayName("Deve retornar produtor rural logado com sucesso (GET /farm-owners/me)")
    void testGetLoggedInFarmOwnerSuccess() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));

        FarmOwnerResponseDTO response = farmOwnerService.getLoggedInFarmOwner(farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Sebastião da Silva", response.name());
        verify(farmOwnerRepository, times(1)).findById(1L);
    }

    /**
     * Testa lançamento de erro 401 quando o UserPrincipal for nulo ao buscar produtor logado.
     */
    @Test
    @DisplayName("Deve lançar 401 Unauthorized se principal for nulo em getLoggedInFarmOwner")
    void testGetLoggedInFarmOwnerNullPrincipal() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getLoggedInFarmOwner(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    /**
     * Testa lançamento de erro 403 quando usuário de outro perfil tenta acessar GET /farm-owners/me.
     */
    @Test
    @DisplayName("Deve lançar 403 Forbidden se usuário não for FARM_OWNER em getLoggedInFarmOwner")
    void testGetLoggedInFarmOwnerForbiddenRole() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getLoggedInFarmOwner(admPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Testa lançamento de erro 404 quando o produtor rural logado não for encontrado no banco de dados.
     */
    @Test
    @DisplayName("Deve lançar 404 Not Found se produtor rural logado não for encontrado no banco")
    void testGetLoggedInFarmOwnerNotFound() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getLoggedInFarmOwner(farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Testa listagem de todos os produtores rurais por um administrador sem filtro.
     */
    @Test
    @DisplayName("Deve listar todos os produtores rurais por ADM sem filtro")
    void testGetFarmOwnersByAdmWithoutFilter() {
        when(farmOwnerRepository.findAll()).thenReturn(List.of(sampleFarmOwner));

        List<FarmOwnerResponseDTO> result = farmOwnerService.getFarmOwners(null, admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Sebastião da Silva", result.get(0).name());
        verify(farmOwnerRepository, times(1)).findAll();
    }

    /**
     * Testa listagem de produtores rurais filtrada por fazenda por um administrador.
     */
    @Test
    @DisplayName("Deve listar produtores rurais filtrados por fazenda por ADM")
    void testGetFarmOwnersByAdmWithFarmFilter() {
        when(farmRepository.existsById(10L)).thenReturn(true);
        when(farmOwnerRepository.findAllByIdFarm(10L)).thenReturn(List.of(sampleFarmOwner));

        List<FarmOwnerResponseDTO> result = farmOwnerService.getFarmOwners(10L, admPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).idFarm());
        verify(farmOwnerRepository, times(1)).findAllByIdFarm(10L);
    }

    /**
     * Testa lançamento de 404 quando ADM filtra por fazenda inexistente.
     */
    @Test
    @DisplayName("Deve lançar 404 ao listar produtores com fazenda inexistente por ADM")
    void testGetFarmOwnersByAdmFarmNotFound() {
        when(farmRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getFarmOwners(99L, admPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Testa listagem de produtores rurais por funcionário da empresa integradora (todas as fazendas da empresa).
     */
    @Test
    @DisplayName("Deve listar produtores rurais por Funcionário da integradora")
    void testGetFarmOwnersByCompanyEmployee() {
        when(companyEmployeeRepository.findById(200L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findAllByIdEnterprise(100L)).thenReturn(List.of(sampleFarm));
        when(farmOwnerRepository.findAllByIdFarmIn(List.of(10L))).thenReturn(List.of(sampleFarmOwner));

        List<FarmOwnerResponseDTO> result = farmOwnerService.getFarmOwners(null, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(farmOwnerRepository, times(1)).findAllByIdFarmIn(List.of(10L));
    }

    /**
     * Testa listagem de produtores rurais por funcionário filtrada por fazenda da mesma empresa.
     */
    @Test
    @DisplayName("Deve listar produtores de fazenda específica por Funcionário da integradora")
    void testGetFarmOwnersByCompanyEmployeeWithValidFarm() {
        when(companyEmployeeRepository.findById(200L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findAllByIdEnterprise(100L)).thenReturn(List.of(sampleFarm));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(farmOwnerRepository.findAllByIdFarm(10L)).thenReturn(List.of(sampleFarmOwner));

        List<FarmOwnerResponseDTO> result = farmOwnerService.getFarmOwners(10L, employeePrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(farmOwnerRepository, times(1)).findAllByIdFarm(10L);
    }

    /**
     * Testa lançamento de 403 quando funcionário tenta listar produtores de fazenda de outra integradora.
     */
    @Test
    @DisplayName("Deve lançar 403 quando funcionário tenta listar produtores de fazenda de outra empresa")
    void testGetFarmOwnersByCompanyEmployeeOtherEnterpriseForbidden() {
        Farm otherEnterpriseFarm = Farm.builder().id(20L).idEnterprise(999L).build();
        when(companyEmployeeRepository.findById(200L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findAllByIdEnterprise(100L)).thenReturn(List.of(sampleFarm));
        when(farmRepository.findById(20L)).thenReturn(Optional.of(otherEnterpriseFarm));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getFarmOwners(20L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Testa listagem de produtores rurais por produtor rural na sua própria fazenda.
     */
    @Test
    @DisplayName("Deve listar produtores da mesma fazenda por Produtor Rural")
    void testGetFarmOwnersByFarmOwner() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        when(farmOwnerRepository.findAllByIdFarm(10L)).thenReturn(List.of(sampleFarmOwner));

        List<FarmOwnerResponseDTO> result = farmOwnerService.getFarmOwners(null, farmOwnerPrincipal);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(farmOwnerRepository, times(1)).findAllByIdFarm(10L);
    }

    /**
     * Testa lançamento de 403 quando produtor tenta listar produtores de outra fazenda.
     */
    @Test
    @DisplayName("Deve lançar 403 quando Produtor Rural tenta filtrar por outra fazenda")
    void testGetFarmOwnersByFarmOwnerOtherFarmForbidden() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getFarmOwners(99L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Testa busca bem-sucedida de produtor rural por ID por um usuário administrador.
     */
    @Test
    @DisplayName("Deve buscar produtor rural por ID com sucesso por ADM")
    void testGetFarmOwnerByIdByAdmSuccess() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));

        FarmOwnerResponseDTO response = farmOwnerService.getFarmOwnerById(1L, admPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    /**
     * Testa busca bem-sucedida de produtor rural por ID quando consultado por ele próprio.
     */
    @Test
    @DisplayName("Deve buscar produtor rural por ID com sucesso pelo próprio produtor")
    void testGetFarmOwnerByIdBySelfSuccess() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));

        FarmOwnerResponseDTO response = farmOwnerService.getFarmOwnerById(1L, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    /**
     * Testa lançamento de erro 403 quando um produtor rural tenta acessar dados de outro produtor.
     */
    @Test
    @DisplayName("Deve lançar 403 Forbidden quando produtor rural tentar acessar dados de outro produtor")
    void testGetFarmOwnerByIdOtherFarmOwnerForbidden() {
        FarmOwner otherOwner = FarmOwner.builder()
                .id(2L)
                .name("Outro Produtor")
                .idFarm(10L)
                .build();
        when(farmOwnerRepository.findById(2L)).thenReturn(Optional.of(otherOwner));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getFarmOwnerById(2L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Testa busca bem-sucedida de produtor rural por funcionário da mesma integradora da fazenda.
     */
    @Test
    @DisplayName("Deve buscar produtor rural por ID com sucesso por Funcionário da mesma integradora")
    void testGetFarmOwnerByIdByCompanyEmployeeSuccess() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        when(companyEmployeeRepository.findById(200L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        FarmOwnerResponseDTO response = farmOwnerService.getFarmOwnerById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    /**
     * Testa lançamento de erro 403 quando funcionário tenta consultar produtor de fazenda de outra integradora.
     */
    @Test
    @DisplayName("Deve lançar 403 Forbidden quando funcionário for de outra integradora ao buscar por ID")
    void testGetFarmOwnerByIdByCompanyEmployeeOtherEnterpriseForbidden() {
        sampleFarm.setIdEnterprise(9999L);
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        when(companyEmployeeRepository.findById(200L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getFarmOwnerById(1L, employeePrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    /**
     * Testa lançamento de erro 404 quando o ID do produtor rural pesquisado não existir.
     */
    @Test
    @DisplayName("Deve lançar 404 Not Found ao buscar produtor rural com ID inexistente")
    void testGetFarmOwnerByIdNotFound() {
        when(farmOwnerRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.getFarmOwnerById(99L, admPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Testa atualização completa de e-mail, telefone e senha de produtor rural com sucesso.
     */
    @Test
    @DisplayName("Deve atualizar produtor rural com sucesso (PATCH /farm-owners/{id})")
    void testUpdateFarmOwnerFullSuccess() {
        FarmOwnerUpdateDTO updateDTO = new FarmOwnerUpdateDTO(
                "sebastiao.novo@fazenda.com.br",
                "11999998888",
                "NovaSenha@123"
        );

        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        when(farmOwnerRepository.findByEmailIgnoreCase("sebastiao.novo@fazenda.com.br")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("new_encoded_pwd");
        when(farmOwnerRepository.save(any(FarmOwner.class))).thenReturn(sampleFarmOwner);

        FarmOwnerResponseDTO response = farmOwnerService.updateFarmOwner(1L, updateDTO, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals("sebastiao.novo@fazenda.com.br", sampleFarmOwner.getEmail());
        assertEquals("11999998888", sampleFarmOwner.getTelephone());
        assertEquals("new_encoded_pwd", sampleFarmOwner.getPassword());
        verify(farmOwnerRepository, times(1)).save(sampleFarmOwner);
    }

    /**
     * Testa atualização mantendo o mesmo e-mail pertencente ao próprio produtor.
     */
    @Test
    @DisplayName("Deve atualizar produtor rural mantendo o mesmo e-mail dele próprio")
    void testUpdateFarmOwnerKeepingSameEmail() {
        FarmOwnerUpdateDTO updateDTO = new FarmOwnerUpdateDTO(
                "sebastiao.silva@fazenda.com.br",
                "11999998888",
                null
        );

        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        when(farmOwnerRepository.findByEmailIgnoreCase("sebastiao.silva@fazenda.com.br")).thenReturn(Optional.of(sampleFarmOwner));
        when(farmOwnerRepository.save(any(FarmOwner.class))).thenReturn(sampleFarmOwner);

        FarmOwnerResponseDTO response = farmOwnerService.updateFarmOwner(1L, updateDTO, farmOwnerPrincipal);

        assertNotNull(response);
        assertEquals("11999998888", sampleFarmOwner.getTelephone());
        verify(farmOwnerRepository, times(1)).save(sampleFarmOwner);
    }

    /**
     * Testa retorno dos dados existentes sem acionar o repositório quando o payload não tiver atualizações.
     */
    @Test
    @DisplayName("Deve retornar dados existentes sem salvar quando payload não contiver atualizações")
    void testUpdateFarmOwnerWithoutUpdates() {
        FarmOwnerUpdateDTO updateDTO = new FarmOwnerUpdateDTO(null, null, null);

        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));

        FarmOwnerResponseDTO response = farmOwnerService.updateFarmOwner(1L, updateDTO, farmOwnerPrincipal);

        assertNotNull(response);
        verify(farmOwnerRepository, never()).save(any());
    }

    /**
     * Testa lançamento de erro 409 quando o novo e-mail já pertencer a outro produtor rural.
     */
    @Test
    @DisplayName("Deve lançar 409 Conflict ao atualizar para e-mail já utilizado por outro produtor")
    void testUpdateFarmOwnerDuplicateEmail() {
        FarmOwner otherOwner = FarmOwner.builder()
                .id(2L)
                .email("outro@fazenda.com.br")
                .build();

        FarmOwnerUpdateDTO updateDTO = new FarmOwnerUpdateDTO(
                "outro@fazenda.com.br",
                null,
                null
        );

        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        when(farmOwnerRepository.findByEmailIgnoreCase("outro@fazenda.com.br")).thenReturn(Optional.of(otherOwner));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.updateFarmOwner(1L, updateDTO, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("e-mail"));
        verify(farmOwnerRepository, never()).save(any());
    }

    /**
     * Testa remoção bem-sucedida de produtor rural por um administrador global.
     */
    @Test
    @DisplayName("Deve remover produtor rural com sucesso por ADM")
    void testDeleteFarmOwnerByAdmSuccess() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        doNothing().when(farmOwnerRepository).delete(sampleFarmOwner);

        assertDoesNotThrow(() -> farmOwnerService.deleteFarmOwner(1L, admPrincipal));

        verify(farmOwnerRepository, times(1)).delete(sampleFarmOwner);
        verify(farmOwnerRepository, times(1)).flush();
    }

    /**
     * Testa remoção bem-sucedida de produtor rural por funcionário da integradora vinculada à fazenda.
     */
    @Test
    @DisplayName("Deve remover produtor rural com sucesso por Funcionário da mesma integradora")
    void testDeleteFarmOwnerByCompanyEmployeeSuccess() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        when(companyEmployeeRepository.findById(200L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        doNothing().when(farmOwnerRepository).delete(sampleFarmOwner);

        assertDoesNotThrow(() -> farmOwnerService.deleteFarmOwner(1L, employeePrincipal));

        verify(farmOwnerRepository, times(1)).delete(sampleFarmOwner);
    }

    /**
     * Testa lançamento de erro 403 quando um produtor rural tenta remover cadastro de produtor.
     */
    @Test
    @DisplayName("Deve lançar 403 Forbidden quando Produtor Rural tentar remover produtor")
    void testDeleteFarmOwnerByFarmOwnerForbidden() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.deleteFarmOwner(1L, farmOwnerPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(farmOwnerRepository, never()).delete(any());
    }

    /**
     * Testa lançamento de erro 409 quando há restrição de chave estrangeira ao remover produtor rural.
     */
    @Test
    @DisplayName("Deve lançar 409 Conflict ao remover produtor se houver violação de integridade referencial")
    void testDeleteFarmOwnerDataIntegrityViolation() {
        when(farmOwnerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmOwner));
        doThrow(new DataIntegrityViolationException("Foreign key violation")).when(farmOwnerRepository).flush();

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> farmOwnerService.deleteFarmOwner(1L, admPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("registros vinculados"));
    }
}
