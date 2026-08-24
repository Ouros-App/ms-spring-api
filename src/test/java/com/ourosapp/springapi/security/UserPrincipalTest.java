package com.ourosapp.springapi.security;

import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
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
    void testCreateFromFarmOwner() {
        FarmOwner owner = FarmOwner.builder().id(3L).email("owner@ouros.com").password("pass").build();
        UserPrincipal principal = UserPrincipal.create(owner);

        assertEquals(3L, principal.getId());
        assertEquals("owner@ouros.com", principal.getEmail());
        assertNull(principal.getPassword());
        assertEquals("FARM_OWNER", principal.getRole());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARM_OWNER")));
    }
}
