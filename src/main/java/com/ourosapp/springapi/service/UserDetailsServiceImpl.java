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
