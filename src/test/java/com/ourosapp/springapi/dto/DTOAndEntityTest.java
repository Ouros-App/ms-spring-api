package com.ourosapp.springapi.dto;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.dto.farm.FarmRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmUpdateDTO;
import com.ourosapp.springapi.entity.Address;
import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.security.JwtAuthFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para validação, serialização/desserialização JSON e comportamento de DTOs e entidades.
 */
class DTOAndEntityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * Testa instanciação e acessores do DTO de requisição de login.
     */
    @Test
    void testLoginRequestDTO() {
        LoginRequestDTO dto = new LoginRequestDTO("teste@ouros.com", "senha123");
        assertEquals("teste@ouros.com", dto.email());
        assertEquals("senha123", dto.password());
    }

    /**
     * Testa instanciação e acessores do DTO de resposta de login contendo o token JWT.
     */
    @Test
    void testLoginResponseDTO() {
        LoginResponseDTO dto = new LoginResponseDTO("token123");
        assertEquals("token123", dto.token());
    }

    /**
     * Testa getters, setters, builder e toString da entidade {@link Adm}.
     */
    @Test
    void testAdmEntity() {
        Adm adm = new Adm();
        adm.setId(1L);
        adm.setEmail("adm@ouros.com");
        adm.setPassword("pass");

        assertEquals(1L, adm.getId());
        assertEquals("adm@ouros.com", adm.getEmail());
        assertEquals("pass", adm.getPassword());

        Adm built = Adm.builder().id(2L).email("adm2@ouros.com").password("pass2").build();
        assertEquals(2L, built.getId());
        assertTrue(built.toString().contains("adm2@ouros.com"));
        assertFalse(built.toString().contains("pass2"));
    }

    /**
     * Testa getters, setters, builder e toString da entidade {@link CompanyEmployee}.
     */
    @Test
    void testCompanyEmployeeEntity() {
        CompanyEmployee emp = new CompanyEmployee();
        emp.setId(1L);
        emp.setName("João");
        emp.setDocumentNumber("12345678901");
        emp.setEmail("joao@empresa.com");
        emp.setTelephone("11999999999");
        emp.setPassword("pass");
        emp.setIdEnterprise(10L);

        assertEquals(1L, emp.getId());
        assertEquals("João", emp.getName());
        assertEquals("12345678901", emp.getDocumentNumber());
        assertEquals("joao@empresa.com", emp.getEmail());
        assertEquals("11999999999", emp.getTelephone());
        assertEquals("pass", emp.getPassword());
        assertEquals(10L, emp.getIdEnterprise());

        CompanyEmployee built = CompanyEmployee.builder()
                .id(2L)
                .name("Maria")
                .documentNumber("98765432100")
                .email("maria@empresa.com")
                .telephone("11888888888")
                .password("pass2")
                .idEnterprise(20L)
                .build();
        assertEquals("Maria", built.getName());
        assertTrue(built.toString().contains("Maria"));
        assertFalse(built.toString().contains("pass2"));
    }

    /**
     * Testa getters, setters, builder e toString da entidade {@link FarmOwner}.
     */
    @Test
    void testFarmOwnerEntity() {
        FarmOwner owner = new FarmOwner();
        owner.setId(1L);
        owner.setName("Carlos");
        owner.setDocumentNumber("11122233344");
        owner.setEmail("carlos@fazenda.com");
        owner.setTelephone("11777777777");
        owner.setPassword("pass");
        owner.setIdFarm(5L);

        assertEquals(1L, owner.getId());
        assertEquals("Carlos", owner.getName());
        assertEquals("11122233344", owner.getDocumentNumber());
        assertEquals("carlos@fazenda.com", owner.getEmail());
        assertEquals("11777777777", owner.getTelephone());
        assertEquals("pass", owner.getPassword());
        assertEquals(5L, owner.getIdFarm());

        FarmOwner built = FarmOwner.builder()
                .id(2L)
                .name("Ana")
                .documentNumber("55566677788")
                .email("ana@fazenda.com")
                .telephone("11666666666")
                .password("pass2")
                .idFarm(15L)
                .build();
        assertEquals("Ana", built.getName());
        assertTrue(built.toString().contains("Ana"));
        assertFalse(built.toString().contains("pass2"));
    }

    /**
     * Testa getters, setters e builder da entidade {@link Address}.
     */
    @Test
    void testAddressEntity() {
        Address address = new Address();
        address.setId(1L);
        address.setZipCode("01310-100");
        address.setState("SP");
        address.setCity("São Paulo");
        address.setNumber("1000");
        address.setCountry("BR");

        assertEquals(1L, address.getId());
        assertEquals("01310-100", address.getZipCode());
        assertEquals("SP", address.getState());
        assertEquals("São Paulo", address.getCity());
        assertEquals("1000", address.getNumber());
        assertEquals("BR", address.getCountry());

        Address built = Address.builder()
                .id(2L)
                .zipCode("13010-001")
                .state("SP")
                .city("Campinas")
                .number("555")
                .country("BR")
                .build();

        assertEquals(2L, built.getId());
        assertEquals("Campinas", built.getCity());
        assertTrue(built.toString().contains("Campinas"));
    }

    /**
     * Testa criação, normalização e conversão a partir de entidade para os DTOs de endereço.
     */
    @Test
    void testAddressDTOs() {
        AddressUpdateDTO request = new AddressUpdateDTO("01310-100", "SP", "São Paulo", "1000", "BR");
        assertEquals("01310-100", request.zipCode());
        assertEquals("SP", request.state());
        assertEquals("São Paulo", request.city());
        assertEquals("1000", request.number());
        assertEquals("BR", request.country());
        assertTrue(request.hasUpdates());

        // Test individual hasUpdates branches
        assertTrue(new AddressUpdateDTO("12345", null, null, null, null).hasUpdates());
        assertTrue(new AddressUpdateDTO(null, "SP", null, null, null).hasUpdates());
        assertTrue(new AddressUpdateDTO(null, null, "City", null, null).hasUpdates());
        assertTrue(new AddressUpdateDTO(null, null, null, "10", null).hasUpdates());
        assertTrue(new AddressUpdateDTO(null, null, null, null, "BR").hasUpdates());

        AddressUpdateDTO emptyUpdate = new AddressUpdateDTO(null, null, null, null, null);
        assertFalse(emptyUpdate.hasUpdates());
        assertNull(emptyUpdate.zipCode());
        assertNull(emptyUpdate.state());
        assertNull(emptyUpdate.city());
        assertNull(emptyUpdate.number());
        assertNull(emptyUpdate.country());

        AddressUpdateDTO blankUpdate = new AddressUpdateDTO("   ", "   ", "   ", "   ", "   ");
        assertFalse(blankUpdate.hasUpdates());

        AddressUpdateDTO normalizedUpdate = new AddressUpdateDTO("  12345678  ", " sp ", "  São Paulo  ", " 1000 ", " br ");
        assertEquals("12345678", normalizedUpdate.zipCode());
        assertEquals("SP", normalizedUpdate.state());
        assertEquals("São Paulo", normalizedUpdate.city());
        assertEquals("1000", normalizedUpdate.number());
        assertEquals("BR", normalizedUpdate.country());

        AddressRequestDTO normalizedRequest = new AddressRequestDTO("  01310-100  ", " sp ", "  São Paulo  ", " 1000 ", " br ");
        assertEquals("01310-100", normalizedRequest.zipCode());
        assertEquals("SP", normalizedRequest.state());
        assertEquals("São Paulo", normalizedRequest.city());
        assertEquals("1000", normalizedRequest.number());
        assertEquals("BR", normalizedRequest.country());

        AddressRequestDTO nullRequest = new AddressRequestDTO(null, null, null, null, null);
        assertNull(nullRequest.zipCode());
        assertNull(nullRequest.state());
        assertNull(nullRequest.city());
        assertNull(nullRequest.number());
        assertNull(nullRequest.country());

        Address entity = Address.builder()
                .id(10L)
                .zipCode("01310-100")
                .state("SP")
                .city("São Paulo")
                .number("1000")
                .country("BR")
                .build();

        AddressResponseDTO response = AddressResponseDTO.fromEntity(entity);
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("01310-100", response.zipCode());
        assertEquals("SP", response.state());
        assertEquals("São Paulo", response.city());
        assertEquals("1000", response.number());
        assertEquals("BR", response.country());

        assertThrows(NullPointerException.class, () -> AddressResponseDTO.fromEntity(null));
    }

    @Test
    void testConstantsClasses() throws Exception {
        assertEquals("ADM", com.ourosapp.springapi.constants.RoleConstants.ADM);
        assertEquals("COMPANY_EMPLOYEE", com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE);
        assertEquals("Funcionário não encontrado", com.ourosapp.springapi.constants.ErrorMessages.EMPLOYEE_NOT_FOUND);
        assertEquals("Usuário não autenticado", com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED);

        // Exercise private constructors for 100% coverage
        var roleConstConstructor = com.ourosapp.springapi.constants.RoleConstants.class.getDeclaredConstructor();
        roleConstConstructor.setAccessible(true);
        roleConstConstructor.newInstance();

        var errorMsgConstructor = com.ourosapp.springapi.constants.ErrorMessages.class.getDeclaredConstructor();
        errorMsgConstructor.setAccessible(true);
        errorMsgConstructor.newInstance();
    }

    /**
     * Testa criação dos beans de segurança PasswordEncoder e CorsConfigurationSource.
     */
    @Test
    void testSecurityConfigBeans() {
        JwtAuthFilter filter = Mockito.mock(JwtAuthFilter.class);
        SecurityConfig config = new SecurityConfig(filter);

        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder);
        assertTrue(encoder.matches("123", encoder.encode("123")));

        CorsConfigurationSource cors = config.corsConfigurationSource();
        assertNotNull(cors);
    }

    /**
     * Testa getters, setters e builder da entidade {@link Enterprise}.
     */
    @Test
    void testEnterpriseEntity() {
        Enterprise enterprise = new Enterprise();
        enterprise.setId(1L);
        enterprise.setName("Agro Ouros S.A.");
        enterprise.setEmail("contato@agroouros.com.br");
        enterprise.setDocumentNumber("12345678000195");
        enterprise.setTelephone("11999999999");
        enterprise.setIdAddress(10L);

        assertEquals(1L, enterprise.getId());
        assertEquals("Agro Ouros S.A.", enterprise.getName());
        assertEquals("contato@agroouros.com.br", enterprise.getEmail());
        assertEquals("12345678000195", enterprise.getDocumentNumber());
        assertEquals("11999999999", enterprise.getTelephone());
        assertEquals(10L, enterprise.getIdAddress());

        Enterprise built = Enterprise.builder()
                .id(2L)
                .name("Ouros Filial")
                .email("filial@agroouros.com.br")
                .documentNumber("98765432000100")
                .telephone("11888888888")
                .idAddress(20L)
                .build();

        assertEquals(2L, built.getId());
        assertEquals("Ouros Filial", built.getName());
        assertTrue(built.toString().contains("Ouros Filial"));
    }

    /**
     * Testa instanciação, normalização e conversão a partir de entidade dos DTOs de empresa integradora.
     */
    @Test
    void testEnterpriseDTOs() {
        EnterpriseRequestDTO request = new EnterpriseRequestDTO("Agro Ouros S.A.", "contato@agroouros.com.br", "12345678000195", "11999999999", 1L
        , new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR"));
        assertEquals("Agro Ouros S.A.", request.name());
        assertEquals("contato@agroouros.com.br", request.email());
        assertEquals("12345678000195", request.documentNumber());
        assertEquals("11999999999", request.telephone());
        assertEquals(1L, request.idAddress());

        EnterpriseRequestDTO normalizedRequest = new EnterpriseRequestDTO("  Agro Ouros S.A.  ", "  CONTATO@AGROOUROS.COM.BR  ", "  12345678000195  ", "  11999999999  ", 1L
        , new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR"));
        assertEquals("Agro Ouros S.A.", normalizedRequest.name());
        assertEquals("contato@agroouros.com.br", normalizedRequest.email());
        assertEquals("12345678000195", normalizedRequest.documentNumber());
        assertEquals("11999999999", normalizedRequest.telephone());

        EnterpriseRequestDTO nullRequest = new EnterpriseRequestDTO(null, null, null, null, null, null);
        assertNull(nullRequest.name());
        assertNull(nullRequest.email());
        assertNull(nullRequest.documentNumber());
        assertNull(nullRequest.telephone());
        assertNull(nullRequest.idAddress());

        Enterprise entity = Enterprise.builder()
                .id(1L)
                .name("Agro Ouros S.A.")
                .email("contato@agroouros.com.br")
                .documentNumber("12345678000195")
                .telephone("11999999999")
                .idAddress(10L)
                .build();

        EnterpriseResponseDTO response = EnterpriseResponseDTO.fromEntity(entity);
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Agro Ouros S.A.", response.name());
        assertEquals("contato@agroouros.com.br", response.email());
        assertEquals("12345678000195", response.documentNumber());
        assertEquals("11999999999", response.telephone());
        assertEquals(10L, response.idAddress());

        assertThrows(NullPointerException.class, () -> EnterpriseResponseDTO.fromEntity(null));
    }

    /**
     * Testa a desserialização JSON em snake_case e camelCase para {@link EnterpriseRequestDTO}.
     *
     * @throws JsonProcessingException se houver falha de processamento JSON
     */
    @Test
    void testEnterpriseRequestDTOJsonDeserialization() throws JsonProcessingException {
        // Formato snake_case
        String snakeCaseJson = """
                {
                    "name": "Agro Ouros S.A.",
                    "email": "contato@agroouros.com.br",
                    "document_number": "12345678000195",
                    "telephone": "11999999999",
                    "id_address": 10
                }
                """;
        EnterpriseRequestDTO dtoFromSnake = objectMapper.readValue(snakeCaseJson, EnterpriseRequestDTO.class);
        assertEquals("Agro Ouros S.A.", dtoFromSnake.name());
        assertEquals("contato@agroouros.com.br", dtoFromSnake.email());
        assertEquals("12345678000195", dtoFromSnake.documentNumber());
        assertEquals("11999999999", dtoFromSnake.telephone());
        assertEquals(10L, dtoFromSnake.idAddress());

        // Formato camelCase (interoperabilidade via @JsonAlias)
        String camelCaseJson = """
                {
                    "name": "Agro Ouros S.A.",
                    "email": "contato@agroouros.com.br",
                    "documentNumber": "12345678000195",
                    "telephone": "11999999999",
                    "idAddress": 10
                }
                """;
        EnterpriseRequestDTO dtoFromCamel = objectMapper.readValue(camelCaseJson, EnterpriseRequestDTO.class);
        assertEquals("Agro Ouros S.A.", dtoFromCamel.name());
        assertEquals("contato@agroouros.com.br", dtoFromCamel.email());
        assertEquals("12345678000195", dtoFromCamel.documentNumber());
        assertEquals("11999999999", dtoFromCamel.telephone());
        assertEquals(10L, dtoFromCamel.idAddress());
    }

    /**
     * Testa instanciação, normalização e método hasUpdates de {@link EnterpriseUpdateDTO}.
     */
    @Test
    void testEnterpriseUpdateDTO() {
        EnterpriseUpdateDTO updateDTO = new EnterpriseUpdateDTO(
                "  Agro Ouros S.A.  ",
                "  CONTATO@AGROOUROS.COM.BR  ",
                "  12345678000195  ",
                "  11999999999  ",
                2L
        );
        assertEquals("Agro Ouros S.A.", updateDTO.name());
        assertEquals("contato@agroouros.com.br", updateDTO.email());
        assertEquals("12345678000195", updateDTO.documentNumber());
        assertEquals("11999999999", updateDTO.telephone());
        assertEquals(2L, updateDTO.idAddress());
        assertTrue(updateDTO.hasUpdates());

        EnterpriseUpdateDTO emptyDTO = new EnterpriseUpdateDTO(null, null, null, null, null);
        assertFalse(emptyDTO.hasUpdates());

        EnterpriseUpdateDTO blankDTO = new EnterpriseUpdateDTO("  ", "  ", "  ", "  ", null);
        assertFalse(blankDTO.hasUpdates());
    }

    /**
     * Testa instanciação, normalização e conversão de entidade dos DTOs de funcionário da empresa.
     */
    @Test
    void testCompanyEmployeeDTOs() {
        CompanyEmployeeRequestDTO request = new CompanyEmployeeRequestDTO(
                "Carlos Pereira",
                "12345678901",
                "carlos@empresa.com.br",
                "11987654321",
                "Senha@123",
                5L
        );
        assertEquals("Carlos Pereira", request.name());
        assertEquals("12345678901", request.documentNumber());
        assertEquals("carlos@empresa.com.br", request.email());
        assertEquals("11987654321", request.telephone());
        assertEquals("Senha@123", request.password());
        assertEquals(5L, request.idEnterprise());

        CompanyEmployeeRequestDTO normalizedRequest = new CompanyEmployeeRequestDTO(
                "  Carlos Pereira  ",
                "  12345678901  ",
                "  CARLOS@EMPRESA.COM.BR  ",
                "  11987654321  ",
                "  Senha@123  ",
                5L
        );
        assertEquals("Carlos Pereira", normalizedRequest.name());
        assertEquals("12345678901", normalizedRequest.documentNumber());
        assertEquals("carlos@empresa.com.br", normalizedRequest.email());
        assertEquals("11987654321", normalizedRequest.telephone());
        assertEquals("  Senha@123  ", normalizedRequest.password());

        CompanyEmployeeRequestDTO nullRequest = new CompanyEmployeeRequestDTO(null, null, null, null, null, null);
        assertNull(nullRequest.name());
        assertNull(nullRequest.documentNumber());
        assertNull(nullRequest.email());
        assertNull(nullRequest.telephone());
        assertNull(nullRequest.password());
        assertNull(nullRequest.idEnterprise());

        CompanyEmployee entity = CompanyEmployee.builder()
                .id(1L)
                .name("Carlos Pereira")
                .documentNumber("12345678901")
                .email("carlos@empresa.com.br")
                .telephone("11987654321")
                .password("encoded_pass")
                .idEnterprise(5L)
                .build();

        CompanyEmployeeResponseDTO response = CompanyEmployeeResponseDTO.fromEntity(entity);
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Carlos Pereira", response.name());
        assertEquals("12345678901", response.documentNumber());
        assertEquals("carlos@empresa.com.br", response.email());
        assertEquals("11987654321", response.telephone());
        assertEquals(5L, response.idEnterprise());

        assertThrows(NullPointerException.class, () -> CompanyEmployeeResponseDTO.fromEntity(null));
    }

    /**
     * Testa normalização e método hasUpdates do {@link CompanyEmployeeUpdateDTO}.
     */
    @Test
    void testCompanyEmployeeUpdateDTO() {
        CompanyEmployeeUpdateDTO updateDTO = new CompanyEmployeeUpdateDTO(
                "novo@empresa.com.br",
                "11999998888",
                "NovaSenha@123"
        );
        assertEquals("novo@empresa.com.br", updateDTO.email());
        assertEquals("11999998888", updateDTO.telephone());
        assertEquals("NovaSenha@123", updateDTO.password());
        assertTrue(updateDTO.hasUpdates());

        CompanyEmployeeUpdateDTO normalizedUpdate = new CompanyEmployeeUpdateDTO(
                "  NOVO@EMPRESA.COM.BR  ",
                "  11999998888  ",
                "  NovaSenha@123  "
        );
        assertEquals("novo@empresa.com.br", normalizedUpdate.email());
        assertEquals("11999998888", normalizedUpdate.telephone());
        assertEquals("  NovaSenha@123  ", normalizedUpdate.password());

        CompanyEmployeeUpdateDTO emptyUpdate = new CompanyEmployeeUpdateDTO(null, null, null);
        assertFalse(emptyUpdate.hasUpdates());

        CompanyEmployeeUpdateDTO blankUpdate = new CompanyEmployeeUpdateDTO("  ", "  ", "  ");
        assertFalse(blankUpdate.hasUpdates());
    }

    /**
     * Testa desserialização JSON em snake_case e camelCase para {@link CompanyEmployeeRequestDTO}.
     *
     * @throws JsonProcessingException se houver falha de processamento JSON
     */
    @Test
    void testCompanyEmployeeRequestDTOJsonDeserialization() throws JsonProcessingException {
        String json = """
                {
                    "name": "Carlos Pereira",
                    "document_number": "12345678901",
                    "email": "carlos@empresa.com.br",
                    "telephone": "11987654321",
                    "password": "SenhaForte@123",
                    "id_enterprise": 5
                }
                """;
        CompanyEmployeeRequestDTO dto = objectMapper.readValue(json, CompanyEmployeeRequestDTO.class);
        assertEquals("Carlos Pereira", dto.name());
        assertEquals("12345678901", dto.documentNumber());
        assertEquals("carlos@empresa.com.br", dto.email());
        assertEquals("11987654321", dto.telephone());
        assertEquals("SenhaForte@123", dto.password());
        assertEquals(5L, dto.idEnterprise());

        String camelJson = """
                {
                    "name": "Carlos Pereira",
                    "documentNumber": "12345678901",
                    "email": "carlos@empresa.com.br",
                    "telephone": "11987654321",
                    "password": "SenhaForte@123",
                    "idEnterprise": 5
                }
                """;
        CompanyEmployeeRequestDTO camelDto = objectMapper.readValue(camelJson, CompanyEmployeeRequestDTO.class);
        assertEquals("12345678901", camelDto.documentNumber());
        assertEquals(5L, camelDto.idEnterprise());
    }

    /**
     * Testa as validações de restrição e política de complexidade de senha no cadastro de funcionário.
     */
    @Test
    void testCompanyEmployeeRequestDTOPasswordValidation() {
        // Senha válida com 8 caracteres, maiúscula, minúscula, número e caractere especial
        CompanyEmployeeRequestDTO validDto = new CompanyEmployeeRequestDTO(
                "Carlos", "52998224725", "carlos@empresa.com.br", "11987654321", "Senha@12", 1L
        );
        Set<ConstraintViolation<CompanyEmployeeRequestDTO>> violations = validator.validate(validDto);
        assertTrue(violations.isEmpty());

        // Senha curta (< 8)
        CompanyEmployeeRequestDTO shortPass = new CompanyEmployeeRequestDTO(
                "Carlos", "52998224725", "carlos@empresa.com.br", "11987654321", "Sen@12", 1L
        );
        assertFalse(validator.validate(shortPass).isEmpty());

        // Senha longa (> 20)
        CompanyEmployeeRequestDTO longPass = new CompanyEmployeeRequestDTO(
                "Carlos", "52998224725", "carlos@empresa.com.br", "11987654321", "SenhaMuitoLongaComMaisDe20@1", 1L
        );
        assertFalse(validator.validate(longPass).isEmpty());

        // Sem maiúscula
        CompanyEmployeeRequestDTO noUpper = new CompanyEmployeeRequestDTO(
                "Carlos", "52998224725", "carlos@empresa.com.br", "11987654321", "senha@123", 1L
        );
        assertFalse(validator.validate(noUpper).isEmpty());

        // Sem minúscula
        CompanyEmployeeRequestDTO noLower = new CompanyEmployeeRequestDTO(
                "Carlos", "52998224725", "carlos@empresa.com.br", "11987654321", "SENHA@123", 1L
        );
        assertFalse(validator.validate(noLower).isEmpty());

        // Sem número
        CompanyEmployeeRequestDTO noDigit = new CompanyEmployeeRequestDTO(
                "Carlos", "52998224725", "carlos@empresa.com.br", "11987654321", "Senha@abc", 1L
        );
        assertFalse(validator.validate(noDigit).isEmpty());

        // Sem caractere especial
        CompanyEmployeeRequestDTO noSpecial = new CompanyEmployeeRequestDTO(
                "Carlos", "52998224725", "carlos@empresa.com.br", "11987654321", "Senha1234", 1L
        );
        assertFalse(validator.validate(noSpecial).isEmpty());
    }

    /**
     * Testa as validações de senha para o cenário de atualização parcial de funcionário.
     */
    @Test
    void testCompanyEmployeeUpdateDTOPasswordValidation() {
        // Senha válida
        CompanyEmployeeUpdateDTO validDto = new CompanyEmployeeUpdateDTO(null, null, "NovaSenha@123");
        assertTrue(validator.validate(validDto).isEmpty());

        // Senha nula (válido na atualização parcial)
        CompanyEmployeeUpdateDTO nullPass = new CompanyEmployeeUpdateDTO(null, null, null);
        assertTrue(validator.validate(nullPass).isEmpty());

        // Senha vazia (válido na atualização parcial)
        CompanyEmployeeUpdateDTO emptyPass = new CompanyEmployeeUpdateDTO(null, null, "");
        assertTrue(validator.validate(emptyPass).isEmpty());

        // Senha inválida (sem requisitos)
        CompanyEmployeeUpdateDTO invalidPass = new CompanyEmployeeUpdateDTO(null, null, "senha123");
        assertFalse(validator.validate(invalidPass).isEmpty());
    }

    /**
     * Testa getters, setters, builder e toString da entidade {@link Farm}.
     */
    @Test
    void testFarmEntity() {
        Farm farm = new Farm();
        farm.setId(1L);
        farm.setName("Fazenda Teste");
        farm.setAreaProperty(new BigDecimal("120.50"));
        farm.setRegion("Sudeste");
        farm.setPoultryCapacity(40000);
        farm.setPlace("Setor 1");
        farm.setIdAddress(10L);
        farm.setIdEnterprise(20L);

        assertEquals(1L, farm.getId());
        assertEquals("Fazenda Teste", farm.getName());
        assertEquals(new BigDecimal("120.50"), farm.getAreaProperty());
        assertEquals("Sudeste", farm.getRegion());
        assertEquals(40000, farm.getPoultryCapacity());
        assertEquals("Setor 1", farm.getPlace());
        assertEquals(10L, farm.getIdAddress());
        assertEquals(20L, farm.getIdEnterprise());

        Farm built = Farm.builder()
                .id(2L)
                .name("Fazenda Built")
                .areaProperty(new BigDecimal("200.00"))
                .region("Sul")
                .poultryCapacity(50000)
                .place("Setor 2")
                .idAddress(11L)
                .idEnterprise(21L)
                .build();

        assertEquals(2L, built.getId());
        assertTrue(built.toString().contains("Fazenda Built"));
    }

    /**
     * Testa instanciação, sanitização e validação de {@link FarmRequestDTO}.
     */
    @Test
    void testFarmRequestDTO() throws JsonProcessingException {
        // DTO válido com id_address
        FarmRequestDTO validWithId = new FarmRequestDTO(
                "  Fazenda Santa Maria  ",
                new BigDecimal("150.00"),
                "  Sudeste  ",
                50000,
                "  Gleba 1  ",
                1L,
                null,
                2L
        );
        assertEquals("Fazenda Santa Maria", validWithId.name());
        assertEquals("Sudeste", validWithId.region());
        assertEquals("Gleba 1", validWithId.place());
        assertTrue(validator.validate(validWithId).isEmpty());

        // DTO válido com objeto address embutido
        AddressRequestDTO address = new AddressRequestDTO("12345678", "SP", "Campinas", "100", "BR");
        FarmRequestDTO validWithNested = new FarmRequestDTO(
                "Fazenda",
                new BigDecimal("100.00"),
                "Sul",
                1000,
                "Local",
                null,
                address,
                2L
        );
        assertTrue(validator.validate(validWithNested).isEmpty());

        // DTO inválido sem id_address nem address
        FarmRequestDTO noAddress = new FarmRequestDTO(
                "Fazenda",
                new BigDecimal("100.00"),
                "Sul",
                1000,
                "Local",
                null,
                null,
                2L
        );
        assertFalse(validator.validate(noAddress).isEmpty());
        assertFalse(noAddress.hasValidAddressInfo());

        // DTO inválido com ambos id_address e address informados (rejeição de ambiguidade XOR)
        FarmRequestDTO bothAddress = new FarmRequestDTO(
                "Fazenda",
                new BigDecimal("100.00"),
                "Sul",
                1000,
                "Local",
                1L,
                address,
                2L
        );
        assertFalse(validator.validate(bothAddress).isEmpty());
        assertFalse(bothAddress.hasValidAddressInfo());
        assertTrue(validWithId.hasValidAddressInfo());
        assertTrue(validWithNested.hasValidAddressInfo());

        // DTO inválido com id_address negativo (< 0) ou zero (== 0)
        FarmRequestDTO negativeAddressId = new FarmRequestDTO(
                "Fazenda",
                new BigDecimal("100.00"),
                "Sul",
                1000,
                "Local",
                -1L,
                null,
                2L
        );
        assertFalse(validator.validate(negativeAddressId).isEmpty());

        FarmRequestDTO zeroAddressId = new FarmRequestDTO(
                "Fazenda",
                new BigDecimal("100.00"),
                "Sul",
                1000,
                "Local",
                0L,
                null,
                2L
        );
        assertFalse(validator.validate(zeroAddressId).isEmpty());

        // DTO inválido com id_enterprise negativo (< 0) ou zero (== 0)
        FarmRequestDTO negativeEnterpriseId = new FarmRequestDTO(
                "Fazenda",
                new BigDecimal("100.00"),
                "Sul",
                1000,
                "Local",
                1L,
                null,
                -1L
        );
        assertFalse(validator.validate(negativeEnterpriseId).isEmpty());

        FarmRequestDTO zeroEnterpriseId = new FarmRequestDTO(
                "Fazenda",
                new BigDecimal("100.00"),
                "Sul",
                1000,
                "Local",
                1L,
                null,
                0L
        );
        assertFalse(validator.validate(zeroEnterpriseId).isEmpty());

        // Desserialização JSON snake_case
        String snakeJson = """
                {
                    "name": "Fazenda JSON",
                    "area_property": 150.50,
                    "region": "Sudeste",
                    "poultry_capacity": 50000,
                    "place": "Gleba 4",
                    "id_address": 1,
                    "id_enterprise": 2
                }
                """;
        FarmRequestDTO snakeDto = objectMapper.readValue(snakeJson, FarmRequestDTO.class);
        assertEquals("Fazenda JSON", snakeDto.name());
        assertEquals(new BigDecimal("150.50"), snakeDto.areaProperty());
        assertEquals(50000, snakeDto.poultryCapacity());
        assertEquals(1L, snakeDto.idAddress());
        assertEquals(2L, snakeDto.idEnterprise());

        // Desserialização JSON camelCase
        String camelJson = """
                {
                    "name": "Fazenda JSON",
                    "areaProperty": 150.50,
                    "region": "Sudeste",
                    "poultryCapacity": 50000,
                    "place": "Gleba 4",
                    "idAddress": 1,
                    "idEnterprise": 2
                }
                """;
        FarmRequestDTO camelDto = objectMapper.readValue(camelJson, FarmRequestDTO.class);
        assertEquals(1L, camelDto.idAddress());
        assertEquals(2L, camelDto.idEnterprise());
    }

    /**
     * Testa instanciação, serialização e conversão de {@link FarmResponseDTO}.
     */
    @Test
    void testFarmResponseDTO() throws JsonProcessingException {
        Farm farm = Farm.builder()
                .id(1L)
                .name("Fazenda Ouro Verde")
                .areaProperty(new BigDecimal("150.50"))
                .region("Sudeste")
                .poultryCapacity(50000)
                .place("Gleba 4")
                .idAddress(10L)
                .idEnterprise(20L)
                .build();

        FarmResponseDTO dto = FarmResponseDTO.fromEntity(farm);
        assertEquals(1L, dto.id());
        assertEquals("Fazenda Ouro Verde", dto.name());
        assertEquals(new BigDecimal("150.50"), dto.areaProperty());
        assertEquals("Sudeste", dto.region());
        assertEquals(50000, dto.poultryCapacity());
        assertEquals("Gleba 4", dto.place());
        assertEquals(10L, dto.idAddress());
        assertEquals(20L, dto.idEnterprise());

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"area_property\":150.50"));
        assertTrue(json.contains("\"poultry_capacity\":50000"));
        assertTrue(json.contains("\"id_address\":10"));
        assertTrue(json.contains("\"id_enterprise\":20"));

        assertThrows(NullPointerException.class, () -> FarmResponseDTO.fromEntity(null));
    }

    /**
     * Testa comportamento, sanitização e método hasUpdates de {@link FarmUpdateDTO}.
     */
    @Test
    void testFarmUpdateDTO() {
        FarmUpdateDTO dtoWithUpdates = new FarmUpdateDTO(
                "  Novo Nome  ",
                new BigDecimal("200.00"),
                "  Centro-Oeste  ",
                60000,
                "  Gleba 2  "
        );
        assertEquals("Novo Nome", dtoWithUpdates.name());
        assertEquals("Centro-Oeste", dtoWithUpdates.region());
        assertEquals("Gleba 2", dtoWithUpdates.place());
        assertTrue(dtoWithUpdates.hasUpdates());
        assertTrue(validator.validate(dtoWithUpdates).isEmpty());

        FarmUpdateDTO emptyDto = new FarmUpdateDTO(null, null, null, null, null);
        assertFalse(emptyDto.hasUpdates());

        // Validação de valores negativos
        FarmUpdateDTO invalidDto = new FarmUpdateDTO(null, new BigDecimal("-10.00"), null, -5, null);
        assertFalse(validator.validate(invalidDto).isEmpty());

        // Validação de strings em branco/vazias após sanitização
        FarmUpdateDTO blankNameDto = new FarmUpdateDTO("   ", null, null, null, null);
        assertFalse(validator.validate(blankNameDto).isEmpty());

        FarmUpdateDTO blankRegionDto = new FarmUpdateDTO(null, null, "   ", null, null);
        assertFalse(validator.validate(blankRegionDto).isEmpty());

        FarmUpdateDTO blankPlaceDto = new FarmUpdateDTO(null, null, null, null, "   ");
        assertFalse(validator.validate(blankPlaceDto).isEmpty());

        // Desserialização Jackson em snake_case e camelCase (@JsonAlias)
        String snakeJson = "{\"area_property\": 200.00, \"poultry_capacity\": 60000}";
        FarmUpdateDTO fromSnake = assertDoesNotThrow(() -> objectMapper.readValue(snakeJson, FarmUpdateDTO.class));
        assertEquals(new BigDecimal("200.00"), fromSnake.areaProperty());
        assertEquals(60000, fromSnake.poultryCapacity());

        String camelJson = "{\"areaProperty\": 300.00, \"poultryCapacity\": 75000}";
        FarmUpdateDTO fromCamel = assertDoesNotThrow(() -> objectMapper.readValue(camelJson, FarmUpdateDTO.class));
        assertEquals(new BigDecimal("300.00"), fromCamel.areaProperty());
        assertEquals(75000, fromCamel.poultryCapacity());
    }
}


