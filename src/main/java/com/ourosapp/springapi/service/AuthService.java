package com.ourosapp.springapi.service;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AdmRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Serviço responsável pelas regras de negócio de autenticação e login.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Credenciais inválidas.";

    private final AdmRepository admRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private String dummyHash;

    @PostConstruct
    public void init() {
        this.dummyHash = passwordEncoder.encode("ouros-dummy-timing-protection");
    }

    private String getDummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("ouros-dummy-timing-protection");
        }
        return dummyHash;
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    /**
     * Realiza a autenticação de administradores do sistema.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    @Transactional(readOnly = true)
    public LoginResponseDTO loginAdm(LoginRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());
        Optional<Adm> admOpt = admRepository.findByEmailIgnoreCase(normalizedEmail);
        if (admOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), getDummyHash());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        Adm adm = admOpt.get();
        return authenticate(adm.getId(), adm.getEmail(), request.password(), adm.getPassword(), RoleConstants.ADM);
    }

    /**
     * Realiza a autenticação de funcionários da empresa parceira.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    @Transactional(readOnly = true)
    public LoginResponseDTO loginEmployee(LoginRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());
        Optional<CompanyEmployee> employeeOpt = companyEmployeeRepository.findByEmailIgnoreCase(normalizedEmail);
        if (employeeOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), getDummyHash());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        CompanyEmployee employee = employeeOpt.get();
        return authenticate(employee.getId(), employee.getEmail(), request.password(), employee.getPassword(), RoleConstants.COMPANY_EMPLOYEE);
    }

    /**
     * Realiza a autenticação de produtores rurais e proprietários de fazendas.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado e o status de primeiro acesso
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    @Transactional(readOnly = true)
    public LoginResponseDTO loginFarmOwner(LoginRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());
        Optional<FarmOwner> ownerOpt = farmOwnerRepository.findByEmailIgnoreCase(normalizedEmail);
        if (ownerOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), getDummyHash());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        FarmOwner owner = ownerOpt.get();
        return authenticate(owner.getId(), owner.getEmail(), request.password(), owner.getPassword(), RoleConstants.FARM_OWNER, owner.getFirstAccess());
    }

    private LoginResponseDTO authenticate(Long id, String email, String rawPassword, String encodedPassword, String role) {
        return authenticate(id, email, rawPassword, encodedPassword, role, null);
    }

    /**
     * Valida as credenciais e gera a resposta de login com o status de primeiro acesso.
     *
     * @param id identificador do usuário autenticado
     * @param email e-mail do usuário autenticado
     * @param rawPassword senha informada no login
     * @param encodedPassword senha criptografada armazenada
     * @param role perfil de acesso do usuário
     * @param firstAccess indicador opcional de primeiro acesso
     * @return DTO com o token JWT e o indicador de primeiro acesso
     * @throws ResponseStatusException HTTP 401 se a senha for inválida
     */
    private LoginResponseDTO authenticate(Long id, String email, String rawPassword, String encodedPassword, String role, Boolean firstAccess) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        String token = jwtUtil.generateToken(id, email, role);
        return new LoginResponseDTO(token, firstAccess);
    }
}
