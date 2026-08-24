package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AdmRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Credenciais inválidas.";
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5V2I/VbJ/tH6f.Xl9t2DqqV1tQW6e";

    private final AdmRepository admRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Realiza a autenticação de administradores do sistema.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    public LoginResponseDTO loginAdm(LoginRequestDTO request) {
        Optional<Adm> admOpt = admRepository.findByEmail(request.email());
        if (admOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        Adm adm = admOpt.get();
        return authenticate(adm.getId(), adm.getEmail(), request.password(), adm.getPassword(), "ADM");
    }

    /**
     * Realiza a autenticação de funcionários da empresa parceira.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    public LoginResponseDTO loginEmployee(LoginRequestDTO request) {
        Optional<CompanyEmployee> employeeOpt = companyEmployeeRepository.findByEmail(request.email());
        if (employeeOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        CompanyEmployee employee = employeeOpt.get();
        return authenticate(employee.getId(), employee.getEmail(), request.password(), employee.getPassword(), "COMPANY_EMPLOYEE");
    }

    /**
     * Realiza a autenticação de produtores rurais e proprietários de fazendas.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    public LoginResponseDTO loginFarmOwner(LoginRequestDTO request) {
        Optional<FarmOwner> ownerOpt = farmOwnerRepository.findByEmail(request.email());
        if (ownerOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        FarmOwner owner = ownerOpt.get();
        return authenticate(owner.getId(), owner.getEmail(), request.password(), owner.getPassword(), "FARM_OWNER");
    }

    private LoginResponseDTO authenticate(Long id, String email, String rawPassword, String encodedPassword, String role) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        String token = jwtUtil.generateToken(id, email, role);
        return new LoginResponseDTO(token);
    }
}
