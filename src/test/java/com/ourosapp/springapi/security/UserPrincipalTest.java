package com.ourosapp.springapi.security;

import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
    @DisplayName("Deve criar UserPrincipal a partir de Adm com ROLE_ADM")
    void testCreateFromAdm() {
        Adm adm = Adm.builder().id(1L).email("adm@ouros.com").password("pass").build();
        UserPrincipal principal = UserPrincipal.create(adm);

        assertEquals(1L, principal.getId());
        assertEquals("adm@ouros.com", principal.getEmail());
        assertEquals("adm@ouros.com", principal.getUsername());
        assertNull(principal.getPassword());
        assertEquals("ADM", principal.getRole());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
        assertEquals(1, principal.getAuthorities().size());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADM")));
    }

    @Test
    @DisplayName("Deve criar UserPrincipal a partir de CompanyEmployee com ROLE_COMPANY_EMPLOYEE")
    void testCreateFromCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).email("emp@ouros.com").password("pass").build();
        UserPrincipal principal = UserPrincipal.create(employee);

        assertEquals(2L, principal.getId());
        assertEquals("emp@ouros.com", principal.getEmail());
        assertNull(principal.getPassword());
        assertEquals("COMPANY_EMPLOYEE", principal.getRole());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY_EMPLOYEE")));
    }

    @Test
    @DisplayName("Deve criar UserPrincipal a partir de FarmOwner com ROLE_FARM_OWNER")
    void testCreateFromFarmOwner() {
        FarmOwner owner = FarmOwner.builder().id(3L).email("owner@ouros.com").password("pass").build();
        UserPrincipal principal = UserPrincipal.create(owner);

        assertEquals(3L, principal.getId());
        assertEquals("owner@ouros.com", principal.getEmail());
        assertNull(principal.getPassword());
        assertEquals("FARM_OWNER", principal.getRole());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARM_OWNER")));
    }

    @Test
    @DisplayName("Deve retornar lista vazia e não nula quando instanciado sem authorities")
    void testAuthoritiesNonNullFallback() {
        UserPrincipal principalFromConvenienceConstructor = new UserPrincipal(
                10L, "test@ouros.com", null, "ADM", null
        );
        assertNotNull(principalFromConvenienceConstructor.getAuthorities());
        assertTrue(principalFromConvenienceConstructor.getAuthorities().isEmpty());

        UserPrincipal principalFromBuilder = UserPrincipal.builder()
                .id(10L)
                .email("test@ouros.com")
                .role("ADM")
                .authorities(null)
                .build();
        assertNotNull(principalFromBuilder.getAuthorities());
        assertTrue(principalFromBuilder.getAuthorities().isEmpty());
    }
}
