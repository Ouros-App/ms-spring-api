package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.EMPLOYEE_NOT_FOUND;
import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;

import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeRequestDTO;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeResponseDTO;
import com.ourosapp.springapi.dto.companyemployee.CompanyEmployeeUpdateDTO;
import com.ourosapp.springapi.entity.Adm;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.repository.AdmRepository;
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
    private final AdmRepository admRepository;

    @Transactional
    public CompanyEmployeeResponseDTO createCompanyEmployee(CompanyEmployeeRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        if (principal != null) {
            ensureAuthenticated(principal);
            validateCompanyEmployeeCreationPermission(request.idEnterprise(), principal);
        }

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
        if (ADM.equals(principal.getRole())) {
            Adm adm = null;
            if (principal.getId() != null) {
                adm = admRepository.findById(principal.getId()).orElse(null);
            }
            if (adm == null && principal.getEmail() != null) {
                adm = admRepository.findByEmailIgnoreCase(principal.getEmail()).orElse(null);
            }
            String name = (adm != null && adm.getName() != null && !adm.getName().isBlank())
                    ? adm.getName()
                    : "Administrador da Plataforma";
            String email = (adm != null && adm.getEmail() != null)
                    ? adm.getEmail()
                    : principal.getEmail();
            String photoUrl = adm != null ? adm.getPhotoUrl() : null;
            return new CompanyEmployeeResponseDTO(
                    principal.getId(),
                    name,
                    "00000000000",
                    email,
                    "11999999999",
                    null,
                    photoUrl
            );
        }
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

        // TODO: Refatorar fluxo para endpoint dedicado PATCH /company-employees/me devido a vulnerabilidade de colisão de IDs.
        if (ADM.equals(principal.getRole())
                && Objects.equals(id, principal.getId())
                && companyEmployeeRepository.findById(id).isEmpty()) {
            return updateAdmOwnProfile(id, request, principal);
        }

        CompanyEmployee employee = companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado para o ID: " + id
                ));

        validateCompanyEmployeeMutationPermission(employee, principal);

        if (!request.hasUpdates()) {
            return CompanyEmployeeResponseDTO.fromEntity(employee);
        }

        if (request.name() != null && !request.name().isBlank()) {
            employee.setName(request.name());
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

        if (request.photoUrl() != null) {
            employee.setPhotoUrl(request.photoUrl().isBlank() ? null : request.photoUrl());
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

    private CompanyEmployeeResponseDTO updateAdmOwnProfile(Long id, CompanyEmployeeUpdateDTO request, UserPrincipal principal) {
        Adm adm = admRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado para o ID: " + id
                ));

        if (request.name() != null && !request.name().isBlank()) {
            adm.setName(request.name());
        }
        if (request.email() != null && !request.email().isBlank()) {
            admRepository.findByEmailIgnoreCase(request.email())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Já existe outro administrador cadastrado com este e-mail"
                        );
                    });
            adm.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            adm.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.photoUrl() != null) {
            adm.setPhotoUrl(request.photoUrl().isBlank() ? null : request.photoUrl());
        }

        Adm savedAdm = admRepository.save(adm);
        String name = (savedAdm.getName() != null && !savedAdm.getName().isBlank())
                ? savedAdm.getName()
                : "Administrador da Plataforma";
        String telephone = (request.telephone() != null && !request.telephone().isBlank())
                ? request.telephone()
                : "11999999999";

        return new CompanyEmployeeResponseDTO(
                savedAdm.getId(),
                name,
                "00000000000",
                savedAdm.getEmail() != null ? savedAdm.getEmail() : principal.getEmail(),
                telephone,
                null,
                savedAdm.getPhotoUrl()
        );
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
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
