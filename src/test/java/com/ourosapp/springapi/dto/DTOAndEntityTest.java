package com.ourosapp.springapi.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.entity.Address;
import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Enterprise;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.security.JwtAuthFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DTOAndEntityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testLoginRequestDTO() {
        LoginRequestDTO dto = new LoginRequestDTO("teste@ouros.com", "senha123");
        assertEquals("teste@ouros.com", dto.email());
        assertEquals("senha123", dto.password());
    }

    @Test
    void testLoginResponseDTO() {
        LoginResponseDTO dto = new LoginResponseDTO("token123");
        assertEquals("token123", dto.token());
    }

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

    @Test
    void testAddressDTOs() {
        AddressRequestDTO request = new AddressRequestDTO("01310-100", "SP", "São Paulo", "1000", "BR");
        assertEquals("01310-100", request.zipCode());
        assertEquals("SP", request.state());
        assertEquals("São Paulo", request.city());
        assertEquals("1000", request.number());
        assertEquals("BR", request.country());

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
    void testSecurityConfigBeans() {
        JwtAuthFilter filter = Mockito.mock(JwtAuthFilter.class);
        SecurityConfig config = new SecurityConfig(filter);

        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder);
        assertTrue(encoder.matches("123", encoder.encode("123")));

        CorsConfigurationSource cors = config.corsConfigurationSource();
        assertNotNull(cors);
    }

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

    @Test
    void testEnterpriseDTOs() {
        EnterpriseRequestDTO request = new EnterpriseRequestDTO(
                "Agro Ouros S.A.",
                "contato@agroouros.com.br",
                "12345678000195",
                "11999999999",
                1L
        );
        assertEquals("Agro Ouros S.A.", request.name());
        assertEquals("contato@agroouros.com.br", request.email());
        assertEquals("12345678000195", request.documentNumber());
        assertEquals("11999999999", request.telephone());
        assertEquals(1L, request.idAddress());

        EnterpriseRequestDTO normalizedRequest = new EnterpriseRequestDTO(
                "  Agro Ouros S.A.  ",
                "  CONTATO@AGROOUROS.COM.BR  ",
                "  12345678000195  ",
                "  11999999999  ",
                1L
        );
        assertEquals("Agro Ouros S.A.", normalizedRequest.name());
        assertEquals("contato@agroouros.com.br", normalizedRequest.email());
        assertEquals("12345678000195", normalizedRequest.documentNumber());
        assertEquals("11999999999", normalizedRequest.telephone());

        EnterpriseRequestDTO nullRequest = new EnterpriseRequestDTO(null, null, null, null, null);
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

    @Test
    void testCompanyEmployeeRequestDTOPasswordValidation() {
        // Senha válida com 8 caracteres, maiúscula, minúscula, número e caractere especial
        CompanyEmployeeRequestDTO validDto = new CompanyEmployeeRequestDTO(
                "Carlos", "12345678901", "carlos@empresa.com.br", "11987654321", "Senha@12", 1L
        );
        Set<ConstraintViolation<CompanyEmployeeRequestDTO>> violations = validator.validate(validDto);
        assertTrue(violations.isEmpty());

        // Senha curta (< 8)
        CompanyEmployeeRequestDTO shortPass = new CompanyEmployeeRequestDTO(
                "Carlos", "12345678901", "carlos@empresa.com.br", "11987654321", "Sen@12", 1L
        );
        assertFalse(validator.validate(shortPass).isEmpty());

        // Senha longa (> 20)
        CompanyEmployeeRequestDTO longPass = new CompanyEmployeeRequestDTO(
                "Carlos", "12345678901", "carlos@empresa.com.br", "11987654321", "SenhaMuitoLongaComMaisDe20@1", 1L
        );
        assertFalse(validator.validate(longPass).isEmpty());

        // Sem maiúscula
        CompanyEmployeeRequestDTO noUpper = new CompanyEmployeeRequestDTO(
                "Carlos", "12345678901", "carlos@empresa.com.br", "11987654321", "senha@123", 1L
        );
        assertFalse(validator.validate(noUpper).isEmpty());

        // Sem minúscula
        CompanyEmployeeRequestDTO noLower = new CompanyEmployeeRequestDTO(
                "Carlos", "12345678901", "carlos@empresa.com.br", "11987654321", "SENHA@123", 1L
        );
        assertFalse(validator.validate(noLower).isEmpty());

        // Sem número
        CompanyEmployeeRequestDTO noDigit = new CompanyEmployeeRequestDTO(
                "Carlos", "12345678901", "carlos@empresa.com.br", "11987654321", "Senha@abc", 1L
        );
        assertFalse(validator.validate(noDigit).isEmpty());

        // Sem caractere especial
        CompanyEmployeeRequestDTO noSpecial = new CompanyEmployeeRequestDTO(
                "Carlos", "12345678901", "carlos@empresa.com.br", "11987654321", "Senha1234", 1L
        );
        assertFalse(validator.validate(noSpecial).isEmpty());
    }

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
}

