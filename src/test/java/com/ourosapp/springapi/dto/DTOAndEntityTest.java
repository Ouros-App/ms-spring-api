package com.ourosapp.springapi.dto;

import com.ourosapp.springapi.config.SecurityConfig;
import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.security.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class DTOAndEntityTest {

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
}
