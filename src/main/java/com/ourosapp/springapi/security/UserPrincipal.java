package com.ourosapp.springapi.security;

import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Implementação customizada de {@link UserDetails} do Spring Security.
 * Encapsula as informações do usuário autenticado (ID, e-mail, perfil e autoridades)
 * extraídas de diferentes entidades do sistema (Adm, CompanyEmployee, FarmOwner).
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Cria um {@link UserPrincipal} a partir de uma entidade {@link Adm}.
     *
     * @param adm a entidade de administrador
     * @return instância de UserPrincipal com ROLE_ADM
     */
    public static UserPrincipal create(Adm adm) {
        return new UserPrincipal(
                adm.getId(),
                adm.getEmail(),
                null,
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
    }

    /**
     * Cria um {@link UserPrincipal} a partir de uma entidade {@link CompanyEmployee}.
     *
     * @param employee a entidade de funcionário da empresa
     * @return instância de UserPrincipal com ROLE_COMPANY_EMPLOYEE
     */
    public static UserPrincipal create(CompanyEmployee employee) {
        return new UserPrincipal(
                employee.getId(),
                employee.getEmail(),
                null,
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );
    }

    /**
     * Cria um {@link UserPrincipal} a partir de uma entidade {@link FarmOwner}.
     *
     * @param farmOwner a entidade de produtor rural / proprietário da fazenda
     * @return instância de UserPrincipal com ROLE_FARM_OWNER
     */
    public static UserPrincipal create(FarmOwner farmOwner) {
        return new UserPrincipal(
                farmOwner.getId(),
                farmOwner.getEmail(),
                null,
                "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }

    /**
     * Retorna o nome de usuário usado para autenticação (neste caso, o e-mail).
     *
     * @return o e-mail do usuário
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indica se a conta do usuário não está expirada.
     *
     * @return sempre {@code true} nesta implementação
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica se a conta do usuário não está bloqueada.
     *
     * @return sempre {@code true} nesta implementação
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indica se as credenciais do usuário não estão expiradas.
     *
     * @return sempre {@code true} nesta implementação
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica se a conta do usuário está habilitada.
     *
     * @return sempre {@code true} nesta implementação
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
