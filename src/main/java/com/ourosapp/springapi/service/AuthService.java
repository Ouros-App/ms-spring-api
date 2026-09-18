package com.ourosapp.springapi.service;

import com.ourosapp.springapi.client.auth.AuthServiceClient;
import com.ourosapp.springapi.client.auth.dto.AuthVerifyResponseDTO;
import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.LoginRequestDTO;
import com.ourosapp.springapi.dto.LoginResponseDTO;
import com.ourosapp.springapi.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serviço responsável pelas regras de negócio de autenticação e login,
 * delegando a validação de credenciais ao ms-auth-service (Marco M2).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Credenciais inválidas.";

    private final AuthServiceClient authServiceClient;
    private final JwtUtil jwtUtil;

    /**
     * Realiza a autenticação de administradores do sistema via ms-auth-service.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    public LoginResponseDTO loginAdm(LoginRequestDTO request) {
        AuthVerifyResponseDTO.IdentityDTO identity = authServiceClient
                .verifyCredentials(request.email(), request.password(), "admin")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG));

        String token = jwtUtil.generateToken(identity.id(), identity.email(), RoleConstants.ADM);
        return new LoginResponseDTO(token);
    }

    /**
     * Realiza a autenticação de funcionários da empresa parceira via ms-auth-service.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    public LoginResponseDTO loginEmployee(LoginRequestDTO request) {
        AuthVerifyResponseDTO.IdentityDTO identity = authServiceClient
                .verifyCredentials(request.email(), request.password(), "company_employee")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG));

        String token = jwtUtil.generateToken(identity.id(), identity.email(), RoleConstants.COMPANY_EMPLOYEE);
        return new LoginResponseDTO(token);
    }

    /**
     * Realiza a autenticação de produtores rurais e proprietários de fazendas via ms-auth-service.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado e o status de primeiro acesso
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas
     */
    public LoginResponseDTO loginFarmOwner(LoginRequestDTO request) {
        AuthVerifyResponseDTO.IdentityDTO identity = authServiceClient
                .verifyCredentials(request.email(), request.password(), "farm_owner")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG));

        String token = jwtUtil.generateToken(identity.id(), identity.email(), RoleConstants.FARM_OWNER);
        return new LoginResponseDTO(token, identity.firstAccess());
    }

    /**
     * Realiza a autenticação unificada de qualquer usuário via ms-auth-service com detecção automática do perfil.
     *
     * @param request payload com e-mail e senha
     * @return DTO com o token JWT gerado e o status de primeiro acesso (se aplicável)
     * @throws ResponseStatusException HTTP 401 se credenciais forem inválidas ou HTTP 409 se ambíguas
     */
    public LoginResponseDTO login(LoginRequestDTO request) {
        AuthVerifyResponseDTO.IdentityDTO identity = authServiceClient
                .verifyCredentials(request.email(), request.password(), null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG));

        String role = resolveRole(identity.accountType());
        String token = jwtUtil.generateToken(identity.id(), identity.email(), role);
        return new LoginResponseDTO(token, identity.firstAccess());
    }

    private String resolveRole(String accountType) {
        if ("admin".equalsIgnoreCase(accountType)) {
            return RoleConstants.ADM;
        } else if ("company_employee".equalsIgnoreCase(accountType)) {
            return RoleConstants.COMPANY_EMPLOYEE;
        } else if ("farm_owner".equalsIgnoreCase(accountType)) {
            return RoleConstants.FARM_OWNER;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
    }
}
