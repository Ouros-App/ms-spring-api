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

/**
 * Implementação customizada de {@link UserDetailsService} para autenticação de múltiplos tipos de usuários.
 * Carrega usuários das entidades Adm, CompanyEmployee e FarmOwner baseado no e-mail e role.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AdmRepository admRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Normaliza o e-mail removendo espaços em branco e convertendo para minúsculas.
     *
     * @param email o e-mail a ser normalizado
     * @return o e-mail normalizado ou string vazia se o parâmetro for nulo
     */
    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    /**
     * Carrega o usuário a partir do e-mail e da role específica validada no token JWT,
     * impedindo ambiguidades e riscos de escalação de privilégios entre tabelas.
     *
     * @param email o e-mail do usuário
     * @param role  o perfil de acesso (ADM, COMPANY_EMPLOYEE, FARM_OWNER)
     * @return UserDetails autenticado
     * @throws UsernameNotFoundException se o usuário não for encontrado ou a role for inválida/nula
     */
    public UserDetails loadUserByEmailAndRole(String email, String role) throws UsernameNotFoundException {
        if (role == null || role.isBlank()) {
            throw new UsernameNotFoundException("Token JWT não contém a claim de role necessária.");
        }

        String normalizedEmail = normalizeEmail(email);

        return switch (role) {
            case "ADM" -> admRepository.findByEmailIgnoreCase(normalizedEmail)
                    .map(UserPrincipal::create)
                    .orElseThrow(() -> new UsernameNotFoundException("Administrador não encontrado: " + email));
            case "COMPANY_EMPLOYEE" -> companyEmployeeRepository.findByEmailIgnoreCase(normalizedEmail)
                    .map(UserPrincipal::create)
                    .orElseThrow(() -> new UsernameNotFoundException("Funcionário da empresa não encontrado: " + email));
            case "FARM_OWNER" -> farmOwnerRepository.findByEmailIgnoreCase(normalizedEmail)
                    .map(UserPrincipal::create)
                    .orElseThrow(() -> new UsernameNotFoundException("Produtor rural não encontrado: " + email));
            default -> throw new UsernameNotFoundException("Role desconhecida ou inválida: " + role);
        };
    }

    /**
     * ATENÇÃO: Em cenários onde o mesmo e-mail existe em múltiplas tabelas,
     * a ordem de consulta sequencial prioriza Adm > CompanyEmployee > FarmOwner.
     * Para autenticação segura e sem ambiguidades no contexto de tokens JWT,
     * utilize {@link #loadUserByEmailAndRole(String, String)}.
     *
     * @param email o e-mail do usuário
     * @return UserDetails autenticado
     * @throws UsernameNotFoundException se o usuário não for encontrado em nenhuma das tabelas
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = normalizeEmail(email);

        Optional<Adm> adm = admRepository.findByEmailIgnoreCase(normalizedEmail);
        if (adm.isPresent()) {
            return UserPrincipal.create(adm.get());
        }

        Optional<CompanyEmployee> employee = companyEmployeeRepository.findByEmailIgnoreCase(normalizedEmail);
        if (employee.isPresent()) {
            return UserPrincipal.create(employee.get());
        }

        Optional<FarmOwner> owner = farmOwnerRepository.findByEmailIgnoreCase(normalizedEmail);
        if (owner.isPresent()) {
            return UserPrincipal.create(owner.get());
        }
        throw new UsernameNotFoundException("Usuário não encontrado ou inativo no banco de dados: " + email);
    }
}
