package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.category.CategoryRequestDTO;
import com.ourosapp.springapi.dto.category.CategoryResponseDTO;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.repository.*;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link CategoryService}.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TipRepository tipRepository;

    @Mock
    private TipCategoryRepository tipCategoryRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @InjectMocks
    private CategoryService categoryService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal farmOwnerPrincipal;
    private Tip sampleTip;
    private Farm sampleFarm;
    private CompanyEmployee sampleEmployee;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(
                1L, "admin@ouros.com", "pass", "ADM", List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        employeePrincipal = new UserPrincipal(
                2L, "employee@empresa.com", "pass", "COMPANY_EMPLOYEE", List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        farmOwnerPrincipal = new UserPrincipal(
                3L, "producer@fazenda.com", "pass", "FARM_OWNER", List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );

        sampleFarm = Farm.builder()
                .id(10L)
                .name("Fazenda Boa Esperança")
                .areaProperty(new BigDecimal("200"))
                .region("Centro-Oeste")
                .poultryCapacity(30000)
                .place("Gleba 2")
                .idAddress(1L)
                .idEnterprise(50L)
                .build();

        sampleTip = Tip.builder()
                .id(100L)
                .tip("Controlar temperatura do aviário")
                .idFarm(10L)
                .build();

        sampleEmployee = CompanyEmployee.builder()
                .id(2L)
                .name("Carlos Funcionário")
                .documentNumber("12345678901")
                .email("employee@empresa.com")
                .telephone("11988887777")
                .password("secret")
                .idEnterprise(50L)
                .build();
    }

    @Test
    @DisplayName("createCategory - Deve cadastrar categoria com sucesso quando solicitado por ADM")
    void deveCadastrarCategoriaComSucessoComoAdm() {
        CategoryRequestDTO request = new CategoryRequestDTO("Ambiência", 100L);
        Category savedCategory = Category.builder()
                .id(1L)
                .category("Ambiência")
                
                .build();

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(tipCategoryRepository.existsByIdTipAndIdCategory(100L, 1L)).thenReturn(false);

        CategoryResponseDTO response = categoryService.createCategory(request, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Ambiência", response.category());
        
        verify(tipCategoryRepository, times(1)).save(any(TipCategory.class));
    }

    @Test
    @DisplayName("createCategory - Deve cadastrar categoria com sucesso quando solicitado por COMPANY_EMPLOYEE da mesma empresa")
    void deveCadastrarCategoriaComSucessoComoCompanyEmployee() {
        CategoryRequestDTO request = new CategoryRequestDTO("Nutrição", 100L);
        Category savedCategory = Category.builder()
                .id(2L)
                .category("Nutrição")
                
                .build();

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(sampleEmployee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(tipCategoryRepository.existsByIdTipAndIdCategory(100L, 2L)).thenReturn(true);

        CategoryResponseDTO response = categoryService.createCategory(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(2L, response.id());
        assertEquals("Nutrição", response.category());
        verify(tipCategoryRepository, never()).save(any(TipCategory.class));
    }

    @Test
    @DisplayName("createCategory - Deve lançar 403 Forbidden quando solicitado por FARM_OWNER")
    void deveLancarForbiddenAoCadastrarCategoriaComoFarmOwner() {
        CategoryRequestDTO request = new CategoryRequestDTO("Nutrição", 100L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                categoryService.createCategory(request, farmOwnerPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createCategory - Deve lançar 401 Unauthorized quando principal for nulo")
    void deveLancarUnauthorizedQuandoPrincipalNulo() {
        CategoryRequestDTO request = new CategoryRequestDTO("Nutrição", 100L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                categoryService.createCategory(request, null)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("createCategory - Deve lançar 404 Not Found quando dica não existir")
    void deveLancarNotFoundQuandoDicaNaoExistir() {
        CategoryRequestDTO request = new CategoryRequestDTO("Nutrição", 999L);
        when(tipRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                categoryService.createCategory(request, adminPrincipal)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("createCategory - Deve lançar 403 Forbidden quando COMPANY_EMPLOYEE tentar vincular dica de outra empresa")
    void deveLancarForbiddenQuandoFuncionarioDeOutraEmpresa() {
        CategoryRequestDTO request = new CategoryRequestDTO("Nutrição", 100L);
        CompanyEmployee otherEmployee = CompanyEmployee.builder()
                .id(2L)
                .idEnterprise(999L)
                .build();

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(companyEmployeeRepository.findById(2L)).thenReturn(Optional.of(otherEmployee));
        when(farmRepository.findById(10L)).thenReturn(Optional.of(sampleFarm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                categoryService.createCategory(request, employeePrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("createCategory - Deve lançar 409 Conflict quando ocorrer DataIntegrityViolationException")
    void deveLancarConflictAoOcorrerViolacaoDeIntegridade() {
        CategoryRequestDTO request = new CategoryRequestDTO("Nutrição", 100L);

        when(tipRepository.findById(100L)).thenReturn(Optional.of(sampleTip));
        when(categoryRepository.save(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DataIntegrityViolationException.class, () ->
                categoryService.createCategory(request, adminPrincipal)
        );
    }

    @Test
    @DisplayName("getCategories - Deve retornar lista de categorias para ADM, COMPANY_EMPLOYEE e FARM_OWNER")
    void deveListarCategoriasParaTodosPerfisAutorizados() {
        List<Category> categories = List.of(
                Category.builder().id(1L).category("Ambiência").build(),
                Category.builder().id(2L).category("Sanitização").build()
        );
        when(categoryRepository.findAll()).thenReturn(categories);

        List<CategoryResponseDTO> resAdm = categoryService.getCategories(adminPrincipal);
        List<CategoryResponseDTO> resEmp = categoryService.getCategories(employeePrincipal);
        List<CategoryResponseDTO> resOwner = categoryService.getCategories(farmOwnerPrincipal);

        assertEquals(2, resAdm.size());
        assertEquals(2, resEmp.size());
        assertEquals(2, resOwner.size());
    }

    @Test
    @DisplayName("getCategories - Deve lançar 403 Forbidden para perfil desconhecido")
    void deveLancarForbiddenParaPerfilDesconhecido() {
        UserPrincipal guestPrincipal = new UserPrincipal(
                99L, "guest@test.com", "pass", "GUEST", List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                categoryService.getCategories(guestPrincipal)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
