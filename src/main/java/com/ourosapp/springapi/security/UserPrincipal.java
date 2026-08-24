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

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
