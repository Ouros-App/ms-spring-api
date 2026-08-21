package com.ourosapp.springapi.service;

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
        when(admRepository.findByEmail("adm@ouros.com")).thenReturn(Optional.of(adm));

        UserDetails userDetails = userDetailsService.loadUserByUsername("adm@ouros.com");

        assertNotNull(userDetails);
        assertEquals("adm@ouros.com", userDetails.getUsername());
    }

    @Test
    void testLoadUserByUsernameCompanyEmployee() {
        CompanyEmployee employee = CompanyEmployee.builder().id(2L).email("emp@ouros.com").password("pass").build();
        when(admRepository.findByEmail("emp@ouros.com")).thenReturn(Optional.empty());
        when(companyEmployeeRepository.findByEmail("emp@ouros.com")).thenReturn(Optional.of(employee));

        UserDetails userDetails = userDetailsService.loadUserByUsername("emp@ouros.com");

        assertNotNull(userDetails);
        assertEquals("emp@ouros.com", userDetails.getUsername());
    }

    @Test
    void testLoadUserByUsernameFarmOwner() {
        FarmOwner owner = FarmOwner.builder().id(3L).email("farmer@ouros.com").password("pass").build();
        when(admRepository.findByEmail("farmer@ouros.com")).thenReturn(Optional.empty());
        when(companyEmployeeRepository.findByEmail("farmer@ouros.com")).thenReturn(Optional.empty());
        when(farmOwnerRepository.findByEmail("farmer@ouros.com")).thenReturn(Optional.of(owner));

        UserDetails userDetails = userDetailsService.loadUserByUsername("farmer@ouros.com");

        assertNotNull(userDetails);
        assertEquals("farmer@ouros.com", userDetails.getUsername());
    }

    @Test
    void testLoadUserByUsernameNotFound() {
        when(admRepository.findByEmail("notfound@ouros.com")).thenReturn(Optional.empty());
        when(companyEmployeeRepository.findByEmail("notfound@ouros.com")).thenReturn(Optional.empty());
        when(farmOwnerRepository.findByEmail("notfound@ouros.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("notfound@ouros.com"));
    }
}
