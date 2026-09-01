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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Serviço responsável pela autenticação de usuários e emissão de tokens JWT.
 * Suporta autenticação de Administradores, Funcionários da Empresa e Produtores Rurais.
 * Implementa proteção contra timing attacks através de hash dummy em tentativas de login com e-mail inexistente.
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

    /**
     * Inicializa o hash dummy usado para proteção contra timing attacks.
     * Executado automaticamente após a construção do bean Spring.
     */
    @PostConstruct
    public void init() {
        this.dummyHash = passwordEncoder.encode("ouros-dummy-timing-protection");
    }

    /**
     * Retorna o hash dummy, gerando-o caso ainda não tenha sido inicializado.
     * Usado para manter tempo constante nas operações de autenticação.
     *
     * @return hash dummy BCrypt
     */
    private String getDummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("ouros-dummy-timing-protection");
        }
        return dummyHash;
    }

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
     * Realiza a autenticação de administradores do sistema.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    public LoginResponseDTO loginAdm(LoginRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());
        Optional<Adm> admOpt = admRepository.findByEmailIgnoreCase(normalizedEmail);
        if (admOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), getDummyHash());
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
        String normalizedEmail = normalizeEmail(request.email());
        Optional<CompanyEmployee> employeeOpt = companyEmployeeRepository.findByEmailIgnoreCase(normalizedEmail);
        if (employeeOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), getDummyHash());
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
        String normalizedEmail = normalizeEmail(request.email());
        Optional<FarmOwner> ownerOpt = farmOwnerRepository.findByEmailIgnoreCase(normalizedEmail);
        if (ownerOpt.isEmpty()) {
            passwordEncoder.matches(request.password(), getDummyHash());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        FarmOwner owner = ownerOpt.get();
        return authenticate(owner.getId(), owner.getEmail(), request.password(), owner.getPassword(), "FARM_OWNER");
    }

    /**
     * Método auxiliar que valida a senha e gera o token JWT em caso de sucesso.
     *
     * @param id              ID do usuário
     * @param email           e-mail do usuário
     * @param rawPassword     senha fornecida em texto plano
     * @param encodedPassword senha hash armazenada no banco
     * @param role            perfil de autorização do usuário
     * @return DTO contendo o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se as credenciais forem inválidas
     */
    private LoginResponseDTO authenticate(Long id, String email, String rawPassword, String encodedPassword, String role) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }
        String token = jwtUtil.generateToken(id, email, role);
        return new LoginResponseDTO(token);
    }
}
