package com.ourosapp.springapi.security;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Conversor responsável por extrair claims do token JWT emitido pelo Keycloak,
 * mapear as roles do realm para GrantedAuthorities e instanciar o UserPrincipal no contexto de segurança.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String primaryRole = determinePrimaryRole(authorities);
        String email = extractEmail(jwt);
        String keycloakId = jwt.getSubject();

        UserPrincipal userPrincipal = null;

        if (email != null && primaryRole != null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByEmailAndRole(email, primaryRole);
                if (userDetails instanceof UserPrincipal localPrincipal) {
                    userPrincipal = UserPrincipal.builder()
                            .id(localPrincipal.getId())
                            .keycloakId(keycloakId)
                            .email(localPrincipal.getEmail())
                            .password(null)
                            .role(localPrincipal.getRole())
                            .authorities(authorities)
                            .build();
                }
            } catch (UsernameNotFoundException ex) {
                log.debug("Usuário do Keycloak ({}) com role ({}) não encontrado no banco de dados local: {}", email, primaryRole, ex.getMessage());
            }
        }

        if (userPrincipal == null) {
            userPrincipal = UserPrincipal.builder()
                    .id(null)
                    .keycloakId(keycloakId)
                    .email(email != null ? email : keycloakId)
                    .password(null)
                    .role(primaryRole)
                    .authorities(authorities)
                    .build();
        }

        return new KeycloakAuthenticationToken(jwt, userPrincipal, authorities);
    }

    /**
     * Extrai e normaliza as roles presentes em 'realm_access.roles' e 'resource_access.*.roles'.
     */
    public Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Extrai roles do Realm
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            for (Object roleObj : roles) {
                if (roleObj instanceof String role) {
                    mapRoleToAuthority(role).ifPresent(authorities::add);
                }
            }
        }

        // Extrai roles de Resource (Client) se existirem
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (Object clientObj : resourceAccess.values()) {
                if (clientObj instanceof Map<?, ?> clientMap && clientMap.get("roles") instanceof List<?> clientRoles) {
                    for (Object roleObj : clientRoles) {
                        if (roleObj instanceof String role) {
                            mapRoleToAuthority(role).ifPresent(authorities::add);
                        }
                    }
                }
            }
        }

        // Suporte adicional caso a role venha como claim simples customizada
        String directRole = jwt.getClaimAsString("role");
        if (directRole != null && !directRole.isBlank()) {
            mapRoleToAuthority(directRole).ifPresent(authorities::add);
        }

        return authorities;
    }

    private Optional<GrantedAuthority> mapRoleToAuthority(String role) {
        if (role == null || role.isBlank()) {
            return Optional.empty();
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "ADMIN", "ADM", "ROLE_ADMIN", "ROLE_ADM" ->
                    Optional.of(new SimpleGrantedAuthority("ROLE_ADM"));
            case "COMPANY_EMPLOYEE", "ROLE_COMPANY_EMPLOYEE" ->
                    Optional.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"));
            case "FARM_OWNER", "ROLE_FARM_OWNER" ->
                    Optional.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"));
            default -> Optional.empty();
        };
    }

    private String determinePrimaryRole(Collection<GrantedAuthority> authorities) {
        if (authorities.stream().anyMatch(a -> "ROLE_ADM".equals(a.getAuthority()))) {
            return RoleConstants.ADM;
        }
        if (authorities.stream().anyMatch(a -> "ROLE_COMPANY_EMPLOYEE".equals(a.getAuthority()))) {
            return RoleConstants.COMPANY_EMPLOYEE;
        }
        if (authorities.stream().anyMatch(a -> "ROLE_FARM_OWNER".equals(a.getAuthority()))) {
            return RoleConstants.FARM_OWNER;
        }
        return null;
    }

    private String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase(Locale.ROOT);
        }

        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && preferredUsername.contains("@")) {
            return preferredUsername.trim().toLowerCase(Locale.ROOT);
        }

        return null;
    }
}
