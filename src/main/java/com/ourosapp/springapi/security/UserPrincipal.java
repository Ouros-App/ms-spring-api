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

    // Construtor a partir do Administrador
    public static UserPrincipal create(Adm adm) {
        return new UserPrincipal(
                adm.getId(),
                adm.getEmail(),
                adm.getPassword(),
                "ADM",
                List.of(new SimpleGrantedAuthority("ROLE_ADM"))
        );
    }
    // Construtor a partir do Funcionário da Empresa
    public static UserPrincipal create(CompanyEmployee employee) {
        return new UserPrincipal(
                employee.getId(),
                employee.getEmail(),
                employee.getPassword(),
                "COMPANY_EMPLOYEE",
                List.of(new SimpleGrantedAuthority("ROLE_COMPANY_EMPLOYEE"))
        );
    }
    // Construtor a partir do Dono da Fazenda / Produtor
    public static UserPrincipal create(FarmOwner farmOwner) {
        return new UserPrincipal(
                farmOwner.getId(),
                farmOwner.getEmail(),
                farmOwner.getPassword(),
                "FARM_OWNER",
                List.of(new SimpleGrantedAuthority("ROLE_FARM_OWNER"))
        );
    }

    @Override
    public String getUsername() {
        return email;
    }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
