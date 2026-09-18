package com.ourosapp.springapi.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Token de autenticação do Spring Security representando um usuário autenticado via Keycloak OIDC.
 */
public class KeycloakAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final UserPrincipal principal;

    public KeycloakAuthenticationToken(Jwt jwt, UserPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt != null ? jwt.getTokenValue() : null;
    }

    @Override
    public UserPrincipal getPrincipal() {
        return this.principal;
    }

    public Jwt getJwt() {
        return this.jwt;
    }
}
