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

@Service
@RequiredArgsConstructor
public class AuthService {

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
        Adm adm = admRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
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
        CompanyEmployee employee = companyEmployeeRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
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
        FarmOwner owner = farmOwnerRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
        return authenticate(owner.getId(), owner.getEmail(), request.password(), owner.getPassword(), "FARM_OWNER");
    }

    private LoginResponseDTO authenticate(Long id, String email, String rawPassword, String encodedPassword, String role) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }
        String token = jwtUtil.generateToken(id, email, role);
        return new LoginResponseDTO(token);
    }
}
