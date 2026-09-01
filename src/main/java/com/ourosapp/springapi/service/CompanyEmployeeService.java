package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.CompanyEmployeeRequestDTO;
import com.ourosapp.springapi.dto.CompanyEmployeeResponseDTO;
import com.ourosapp.springapi.dto.CompanyEmployeeUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Serviço responsável pela lógica de negócios e persistência de dados de Funcionários da Empresa Integradora.
 */
@Service
@RequiredArgsConstructor
public class CompanyEmployeeService {

    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cadastra um novo Funcionário da Empresa Integradora no sistema.
     *
     * @param request dados cadastrais do funcionário
     * @return DTO contendo os dados do funcionário cadastrado (sem a senha)
     * @throws ResponseStatusException HTTP 404 (Not Found) se a empresa vinculada não existir
     * @throws ResponseStatusException HTTP 409 (Conflict) se o documento ou e-mail já estiverem cadastrados
     */
    @Transactional
    public CompanyEmployeeResponseDTO createCompanyEmployee(CompanyEmployeeRequestDTO request) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        if (!enterpriseRepository.existsById(request.idEnterprise())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Empresa integradora não encontrada para o ID: " + request.idEnterprise()
            );
        }

        if (companyEmployeeRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um funcionário cadastrado com este documento"
            );
        }

        if (companyEmployeeRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um funcionário cadastrado com este e-mail"
            );
        }

        String encryptedPassword = passwordEncoder.encode(request.password());

        CompanyEmployee employee = CompanyEmployee.builder()
                .name(request.name())
                .documentNumber(request.documentNumber())
                .email(request.email())
                .telephone(request.telephone())
                .password(encryptedPassword)
                .idEnterprise(request.idEnterprise())
                .build();

        try {
            CompanyEmployee savedEmployee = companyEmployeeRepository.save(employee);
            return CompanyEmployeeResponseDTO.fromEntity(savedEmployee);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar funcionário",
                    ex
            );
        }
    }

    /**
     * Busca as informações de um funcionário específico através do seu identificador único.
     *
     * @param id identificador único do funcionário
     * @return DTO com os dados do funcionário encontrado
     * @throws ResponseStatusException HTTP 404 (Not Found) se o funcionário não for encontrado
     */
    @Transactional(readOnly = true)
    public CompanyEmployeeResponseDTO getCompanyEmployeeById(Long id) {
        CompanyEmployee companyEmployee = companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado para o ID: " + id
                ));
        return CompanyEmployeeResponseDTO.fromEntity(companyEmployee);
    }

    /**
     * Retorna as informações do funcionário atualmente autenticado via token JWT.
     *
     * @param principal o usuário autenticado extraído do contexto de segurança
     * @return DTO com os dados do funcionário autenticado
     * @throws ResponseStatusException HTTP 401 (Unauthorized) se o principal for nulo
     * @throws ResponseStatusException HTTP 403 (Forbidden) se o perfil não for de funcionário da empresa
     * @throws ResponseStatusException HTTP 404 (Not Found) se o funcionário não existir no banco
     */
    @Transactional(readOnly = true)
    public CompanyEmployeeResponseDTO getLoggedInEmployee(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        if (!"COMPANY_EMPLOYEE".equals(principal.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso restrito a funcionários da integradora"
            );
        }
        CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário logado não encontrado para o ID: " + principal.getId()
                ));
        return CompanyEmployeeResponseDTO.fromEntity(employee);
    }

    /**
     * Atualiza parcialmente as informações cadastrais de um funcionário existente (telefone, e-mail e/ou senha).
     *
     * @param id      identificador único do funcionário a ser atualizado
     * @param request novos dados parciais a serem aplicados
     * @return DTO com os dados atualizados do funcionário
     * @throws ResponseStatusException HTTP 404 (Not Found) se o funcionário não for encontrado
     * @throws ResponseStatusException HTTP 409 (Conflict) se o novo e-mail já pertencer a outro funcionário
     */
    @Transactional
    public CompanyEmployeeResponseDTO updateCompanyEmployee(Long id, CompanyEmployeeUpdateDTO request) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        CompanyEmployee employee = companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado para o ID: " + id
                ));

        if (request.email() != null && !request.email().isBlank()) {
            companyEmployeeRepository.findByEmailIgnoreCase(request.email())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Já existe outro funcionário cadastrado com este e-mail"
                        );
                    });
            employee.setEmail(request.email());
        }

        if (request.telephone() != null && !request.telephone().isBlank()) {
            employee.setTelephone(request.telephone());
        }

        if (request.password() != null && !request.password().isBlank()) {
            employee.setPassword(passwordEncoder.encode(request.password()));
        }

        try {
            CompanyEmployee updatedEmployee = companyEmployeeRepository.save(employee);
            return CompanyEmployeeResponseDTO.fromEntity(updatedEmployee);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao atualizar funcionário",
                    ex
            );
        }
    }

    /**
     * Remove um funcionário da Empresa Integradora do sistema.
     *
     * @param id identificador único do funcionário a ser removido
     * @throws ResponseStatusException HTTP 404 (Not Found) se o funcionário não for encontrado
     */
    @Transactional
    public void deleteCompanyEmployee(Long id) {
        if (!companyEmployeeRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Funcionário não encontrado para o ID: " + id
            );
        }
        companyEmployeeRepository.deleteById(id);
    }
}

