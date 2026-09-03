package com.ourosapp.springapi.service;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AdmRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private AdmRepository admRepository;

    @Mock
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Mock
    private FarmOwnerRepository farmOwnerRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void testLoadUserByUsernameAdm() {
        Adm adm = Adm.builder().id(1L).email("adm@ouros.com").password("pass").build();
        when(admRepository.findByEmailIgnoreCase("adm@ouros.com")).thenReturn(Optional.of(adm));

        UserDetails userDetails = userDetailsService.loadUserByUsername("adm@ouros.com");

        assertNotNull(userDetails);
        assertEquals("adm@ouros.com", userDetails.getUsername());
    }

    @Test
    void testLoadUserByUsernameCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).email("emp@ouros.com").password("pass").build();
        when(admRepository.findByEmailIgnoreCase("emp@ouros.com")).thenReturn(Optional.empty());
        when(companyEmployeeRepository.findByEmailIgnoreCase("emp@ouros.com")).thenReturn(Optional.of(employee));

        UserDetails userDetails = userDetailsService.loadUserByUsername("emp@ouros.com");

        assertNotNull(userDetails);
        assertEquals("emp@ouros.com", userDetails.getUsername());
    }

    @Test
    void testLoadUserByUsernameFarmOwner() {
        FarmOwner owner = FarmOwner.builder().id(3L).email("farmer@ouros.com").password("pass").build();
        when(admRepository.findByEmailIgnoreCase("farmer@ouros.com")).thenReturn(Optional.empty());
        when(companyEmployeeRepository.findByEmailIgnoreCase("farmer@ouros.com")).thenReturn(Optional.empty());
        when(farmOwnerRepository.findByEmailIgnoreCase("farmer@ouros.com")).thenReturn(Optional.of(owner));

        UserDetails userDetails = userDetailsService.loadUserByUsername("farmer@ouros.com");

        assertNotNull(userDetails);
        assertEquals("farmer@ouros.com", userDetails.getUsername());
    }

    @Test
    void testLoadUserByUsernameNotFound() {
        when(admRepository.findByEmailIgnoreCase("notfound@ouros.com")).thenReturn(Optional.empty());
        when(companyEmployeeRepository.findByEmailIgnoreCase("notfound@ouros.com")).thenReturn(Optional.empty());
        when(farmOwnerRepository.findByEmailIgnoreCase("notfound@ouros.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("notfound@ouros.com"));
    }

    @Test
    void testLoadUserByEmailAndRoleAdm() {
        Adm adm = Adm.builder().id(1L).email("adm@ouros.com").password("pass").build();
        when(admRepository.findByEmailIgnoreCase("adm@ouros.com")).thenReturn(Optional.of(adm));

        UserDetails userDetails = userDetailsService.loadUserByEmailAndRole("ADM@OUROS.COM", "ADM");

        assertNotNull(userDetails);
        assertEquals("adm@ouros.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADM")));
    }

    @Test
    void testLoadUserByEmailAndRoleAdmNotFound() {
        when(admRepository.findByEmailIgnoreCase("adm@ouros.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByEmailAndRole("adm@ouros.com", "ADM"));
    }

    @Test
    void testLoadUserByEmailAndRoleEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).email("emp@ouros.com").password("pass").build();
        when(companyEmployeeRepository.findByEmailIgnoreCase("emp@ouros.com")).thenReturn(Optional.of(employee));

        UserDetails userDetails = userDetailsService.loadUserByEmailAndRole("Emp@Ouros.Com", "COMPANY_EMPLOYEE");

        assertNotNull(userDetails);
        assertEquals("emp@ouros.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY_EMPLOYEE")));
    }

    @Test
    void testLoadUserByEmailAndRoleEmployeeNotFound() {
        when(companyEmployeeRepository.findByEmailIgnoreCase("emp@ouros.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByEmailAndRole("emp@ouros.com", "COMPANY_EMPLOYEE"));
    }

    @Test
    void testLoadUserByEmailAndRoleFarmOwner() {
        FarmOwner owner = FarmOwner.builder().id(3L).email("farmer@ouros.com").password("pass").build();
        when(farmOwnerRepository.findByEmailIgnoreCase("farmer@ouros.com")).thenReturn(Optional.of(owner));

        UserDetails userDetails = userDetailsService.loadUserByEmailAndRole("Farmer@Ouros.COM", "FARM_OWNER");

        assertNotNull(userDetails);
        assertEquals("farmer@ouros.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARM_OWNER")));
    }

    @Test
    void testLoadUserByEmailAndRoleFarmOwnerNotFound() {
        when(farmOwnerRepository.findByEmailIgnoreCase("farmer@ouros.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByEmailAndRole("farmer@ouros.com", "FARM_OWNER"));
    }

    @Test
    void testLoadUserByEmailAndRoleNullRoleThrowsException() {
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByEmailAndRole("adm@ouros.com", null));
    }

    @Test
    void testLoadUserByEmailAndRoleInvalidRoleThrowsException() {
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByEmailAndRole("adm@ouros.com", "INVALID_ROLE"));
    }

    @Test
    void testLoadUserByEmailAndRoleIsolatesAccountsWithSameEmailAcrossDifferentRoles() {
        String sharedEmail = "shared@ouros.com";
        Adm adm = Adm.builder().id(1L).email(sharedEmail).password("pass1").build();
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).email(sharedEmail).password("pass2").build();
        FarmOwner owner = FarmOwner.builder().id(3L).email(sharedEmail).password("pass3").build();

        when(admRepository.findByEmailIgnoreCase(sharedEmail)).thenReturn(Optional.of(adm));
        when(companyEmployeeRepository.findByEmailIgnoreCase(sharedEmail)).thenReturn(Optional.of(employee));
        when(farmOwnerRepository.findByEmailIgnoreCase(sharedEmail)).thenReturn(Optional.of(owner));

        UserDetails admDetails = userDetailsService.loadUserByEmailAndRole(sharedEmail, "ADM");
        UserDetails empDetails = userDetailsService.loadUserByEmailAndRole(sharedEmail, "COMPANY_EMPLOYEE");
        UserDetails farmDetails = userDetailsService.loadUserByEmailAndRole(sharedEmail, "FARM_OWNER");

        assertTrue(admDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADM")));
        assertTrue(empDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY_EMPLOYEE")));
        assertTrue(farmDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARM_OWNER")));
    }
}
