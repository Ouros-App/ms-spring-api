package com.ourosapp.springapi.service;

import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AdmRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AdmRepository admRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Carrega o usuário a partir do e-mail e da role específica validada no token JWT,
     * evitando ambiguidades e problemas de escalação de privilégios quando tabelas diferentes compartilham o mesmo e-mail.
     */
    public UserDetails loadUserByEmailAndRole(String email, String role) throws UsernameNotFoundException {
        if (role == null) {
            return loadUserByUsername(email);
        }

        return switch (role) {
            case "ADM" -> admRepository.findByEmail(email)
                    .map(UserPrincipal::create)
                    .orElseThrow(() -> new UsernameNotFoundException("Administrador não encontrado: " + email));
            case "COMPANY_EMPLOYEE" -> companyEmployeeRepository.findByEmail(email)
                    .map(UserPrincipal::create)
                    .orElseThrow(() -> new UsernameNotFoundException("Funcionário da empresa não encontrado: " + email));
            case "FARM_OWNER" -> farmOwnerRepository.findByEmail(email)
                    .map(UserPrincipal::create)
                    .orElseThrow(() -> new UsernameNotFoundException("Produtor rural não encontrado: " + email));
            default -> loadUserByUsername(email);
        };
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<Adm> adm = admRepository.findByEmail(email);
        if (adm.isPresent()) {
            return UserPrincipal.create(adm.get());
        }

        Optional<CompanyEmployee> employee = companyEmployeeRepository.findByEmail(email);
        if (employee.isPresent()) {
            return UserPrincipal.create(employee.get());
        }

        Optional<FarmOwner> owner = farmOwnerRepository.findByEmail(email);
        if (owner.isPresent()) {
            return UserPrincipal.create(owner.get());
        }
        throw new UsernameNotFoundException("Usuário não encontrado ou inativo no banco de dados: " + email);
    }
}
