package com.ourosapp.springapi.security;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakJwtAuthenticationConverterTest {

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private KeycloakJwtAuthenticationConverter converter;

    @Test
    @DisplayName("Deve converter token com role admin e resolver UserPrincipal local com ID")
    void testConvertAdminSuccess() {
        Jwt jwt = new Jwt(
                "token-admin",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "keycloak-uuid-1",
                        "email", "admin@ouros.com",
                        "realm_access", Map.of("roles", List.of("admin", "default-roles-ouros"))
                )
        );

        UserPrincipal localPrincipal = new UserPrincipal(
                10L,
                "admin@ouros.com",
                null,
                RoleConstants.ADM,
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );

        when(userDetailsService.loadUserByEmailAndRole("admin@ouros.com", RoleConstants.ADM))
                .thenReturn(localPrincipal);

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertNotNull(auth);
        assertInstanceOf(KeycloakAuthenticationToken.class, auth);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(10L, principal.getId());
        assertEquals("keycloak-uuid-1", principal.getKeycloakId());
        assertEquals("admin@ouros.com", principal.getEmail());
        assertEquals(RoleConstants.ADM, principal.getRole());
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADM")));
    }

    @Test
    @DisplayName("Deve converter token com role company_employee e mapear autoridade")
    void testConvertCompanyEmployee() {
        Jwt jwt = new Jwt(
                "token-emp",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "keycloak-uuid-2",
                        "preferred_username", "emp@agro.com",
                        "realm_access", Map.of("roles", List.of("company_employee"))
                )
        );

        UserPrincipal localPrincipal = new UserPrincipal(
                20L,
                "emp@agro.com",
                null,
                RoleConstants.COMPANY_EMPLOYEE,
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );

        when(userDetailsService.loadUserByEmailAndRole("emp@agro.com", RoleConstants.COMPANY_EMPLOYEE))
                .thenReturn(localPrincipal);

        AbstractAuthenticationToken auth = converter.convert(jwt);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        assertEquals(20L, principal.getId());
        assertEquals("emp@agro.com", principal.getEmail());
        assertEquals(RoleConstants.COMPANY_EMPLOYEE, principal.getRole());
    }

    @Test
    @DisplayName("Deve converter token com role farm_owner e tratar fallback quando não encontrado no banco local")
    void testConvertFarmOwnerFallback() {
        Jwt jwt = new Jwt(
                "token-owner",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "keycloak-uuid-3",
                        "email", "owner@fazenda.com",
                        "realm_access", Map.of("roles", List.of("farm_owner"))
                )
        );

        when(userDetailsService.loadUserByEmailAndRole("owner@fazenda.com", RoleConstants.FARM_OWNER))
                .thenThrow(new UsernameNotFoundException("Usuário não encontrado"));

        AbstractAuthenticationToken auth = converter.convert(jwt);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        assertNull(principal.getId());
        assertEquals("keycloak-uuid-3", principal.getKeycloakId());
        assertEquals("owner@fazenda.com", principal.getEmail());
        assertEquals(RoleConstants.FARM_OWNER, principal.getRole());
    }

    @Test
    @DisplayName("Deve extrair database_id da claim do token quando usuário não estiver no banco local")
    void testConvertWithDatabaseIdClaim() {
        Jwt jwt = new Jwt(
                "token-owner-db-id",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "keycloak-uuid-4",
                        "email", "owner2@fazenda.com",
                        "database_id", 42L,
                        "realm_access", Map.of("roles", List.of("farm_owner"))
                )
        );

        when(userDetailsService.loadUserByEmailAndRole("owner2@fazenda.com", RoleConstants.FARM_OWNER))
                .thenThrow(new UsernameNotFoundException("Usuário não encontrado"));

        AbstractAuthenticationToken auth = converter.convert(jwt);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        assertEquals(42L, principal.getId());
        assertEquals("keycloak-uuid-4", principal.getKeycloakId());
        assertEquals("owner2@fazenda.com", principal.getEmail());
        assertEquals(RoleConstants.FARM_OWNER, principal.getRole());
    }

    @Test
    @DisplayName("Deve extrair roles a partir de resource_access para o client configurado")
    void testExtractAuthoritiesFromResourceAccess() {
        Jwt jwt = new Jwt(
                "token-resource",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "uuid-resource",
                        "email", "resource@ouros.com",
                        "resource_access", Map.of("ms-spring-api", Map.of("roles", List.of("admin")))
                )
        );

        var authorities = converter.extractAuthorities(jwt);
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADM")));
    }

    @Test
    @DisplayName("Deve ignorar roles em resource_access de outros clients distintos desta API")
    void testExtractAuthoritiesFromResourceAccessIgnoresOtherClients() {
        Jwt jwt = new Jwt(
                "token-other-client",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "uuid-resource-2",
                        "email", "user@ouros.com",
                        "resource_access", Map.of("other-client-id", Map.of("roles", List.of("admin", "company_employee")))
                )
        );

        var authorities = converter.extractAuthorities(jwt);
        assertTrue(authorities.isEmpty(), "Roles de clients terceiros não devem ser atribuídas a este microserviço");
    }

    @Test
    @DisplayName("Deve permitir configurar client-id customizado e extrair roles dele")
    void testExtractAuthoritiesWithCustomClientId() {
        KeycloakJwtAuthenticationConverter customConverter =
                new KeycloakJwtAuthenticationConverter(userDetailsService, "custom-client");

        Jwt jwt = new Jwt(
                "token-custom",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "uuid-custom",
                        "email", "custom@ouros.com",
                        "resource_access", Map.of(
                                "custom-client", Map.of("roles", List.of("farm_owner")),
                                "ms-spring-api", Map.of("roles", List.of("admin"))
                        )
                )
        );

        var authorities = customConverter.extractAuthorities(jwt);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_FARM_OWNER")));
        assertFalse(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADM")));
    }
}
