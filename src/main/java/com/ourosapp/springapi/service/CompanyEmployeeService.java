package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.EMPLOYEE_NOT_FOUND;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;

import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeRequestDTO;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeResponseDTO;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeUpdateDTO;
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

    @Transactional
    public CompanyEmployeeResponseDTO createCompanyEmployee(CompanyEmployeeRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);
        validateCompanyEmployeeCreationPermission(request.idEnterprise(), principal);

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

    @Transactional(readOnly = true)
    public CompanyEmployeeResponseDTO getCompanyEmployeeById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);
        CompanyEmployee companyEmployee = companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado para o ID: " + id
                ));

        validateCompanyEmployeeAccessPermission(companyEmployee, principal);

        return CompanyEmployeeResponseDTO.fromEntity(companyEmployee);
    }

    @Transactional(readOnly = true)
    public CompanyEmployeeResponseDTO getLoggedInEmployee(UserPrincipal principal) {
        ensureAuthenticated(principal);
        if (!COMPANY_EMPLOYEE.equals(principal.getRole())) {
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

    @Transactional
    public CompanyEmployeeResponseDTO updateCompanyEmployee(Long id, CompanyEmployeeUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        CompanyEmployee employee = companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado para o ID: " + id
                ));

        validateCompanyEmployeeMutationPermission(employee, principal);

        if (!request.hasUpdates()) {
            return CompanyEmployeeResponseDTO.fromEntity(employee);
        }

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

    @Transactional
    public void deleteCompanyEmployee(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);
        CompanyEmployee employee = companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado para o ID: " + id
                ));

        validateCompanyEmployeeMutationPermission(employee, principal);

        companyEmployeeRepository.deleteById(id);
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
    }

    private void validateCompanyEmployeeCreationPermission(Long idEnterprise, UserPrincipal principal) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }
        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EMPLOYEE_NOT_FOUND));
            if (!Objects.equals(idEnterprise, employee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não é permitido cadastrar funcionário em outra empresa");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem permissão para cadastrar funcionários");
    }

    private void validateCompanyEmployeeAccessPermission(CompanyEmployee targetEmployee, UserPrincipal principal) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }
        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee loggedEmployee = companyEmployeeRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, EMPLOYEE_NOT_FOUND));
            if (!Objects.equals(targetEmployee.getIdEnterprise(), loggedEmployee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a este funcionário");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a este funcionário");
    }

    private void validateCompanyEmployeeMutationPermission(CompanyEmployee targetEmployee, UserPrincipal principal) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }
        if (COMPANY_EMPLOYEE.equals(role)) {
            // Apenas o próprio funcionário pode alterar seus dados (ou um ADM da empresa, se existisse essa hierarquia, mas manteremos simples)
            if (!Objects.equals(targetEmployee.getId(), principal.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não é permitido alterar/remover dados de outro funcionário");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para alterar este funcionário");
    }
}
