package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanRequestDTO;
import com.ourosapp.springapi.dto.enterpriseplan.EnterprisePlanResponseDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.EnterprisePlan;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterprisePlanRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.PlanRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pelas regras de negócio e persistência de contratações de planos por empresas integradoras.
 */
@Service
@RequiredArgsConstructor
public class EnterprisePlanService {

    private final EnterprisePlanRepository enterprisePlanRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final PlanRepository planRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;

    @Transactional
    public EnterprisePlanResponseDTO createEnterprisePlan(EnterprisePlanRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Long enterpriseId = resolveAndValidateEnterpriseId(request.idEnterprise(), principal, "contratar planos para");

        if (!enterpriseRepository.existsById(enterpriseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa integradora não encontrada para o ID: " + enterpriseId);
        }

        if (!planRepository.existsById(request.idPlan())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de assinatura não encontrado para o ID: " + request.idPlan());
        }

        if (enterprisePlanRepository.existsByIdEnterpriseAndIdPlan(enterpriseId, request.idPlan())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A empresa integradora já possui este plano de assinatura contratado");
        }

        EnterprisePlan enterprisePlan = EnterprisePlan.builder()
                .idEnterprise(enterpriseId)
                .idPlan(request.idPlan())
                .build();

        try {
            EnterprisePlan saved = enterprisePlanRepository.save(enterprisePlan);
            return EnterprisePlanResponseDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conflito de integridade ao contratar plano para a empresa", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<EnterprisePlanResponseDTO> getEnterprisePlans(Long filterEnterpriseId, Long filterPlanId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (RoleConstants.COMPANY_EMPLOYEE.equals(principal.getRole())) {
            Long userEnterpriseId = getUserEnterpriseIdOrThrow(principal);
            if (filterEnterpriseId != null && !filterEnterpriseId.equals(userEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: colaboradores só podem consultar planos da sua própria empresa");
            }
            filterEnterpriseId = userEnterpriseId;
        } else if (!RoleConstants.ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para este perfil de usuário");
        }

        List<EnterprisePlan> list;
        if (filterEnterpriseId != null && filterPlanId != null) {
            list = enterprisePlanRepository.findByIdEnterpriseAndIdPlan(filterEnterpriseId, filterPlanId).stream().toList();
        } else if (filterEnterpriseId != null) {
            list = enterprisePlanRepository.findByIdEnterprise(filterEnterpriseId);
        } else if (filterPlanId != null) {
            list = enterprisePlanRepository.findByIdPlan(filterPlanId);
        } else {
            list = enterprisePlanRepository.findAll();
        }

        return list.stream().map(EnterprisePlanResponseDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public EnterprisePlanResponseDTO getEnterprisePlanById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        EnterprisePlan ep = findEnterprisePlanByIdOrThrow(id);

        if (RoleConstants.COMPANY_EMPLOYEE.equals(principal.getRole())) {
            Long userEnterpriseId = getUserEnterpriseIdOrThrow(principal);
            if (!ep.getIdEnterprise().equals(userEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: plano contratado não pertence à sua empresa integradora");
            }
        } else if (!RoleConstants.ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para este perfil de usuário");
        }

        return EnterprisePlanResponseDTO.fromEntity(ep);
    }

    @Transactional
    public void deleteEnterprisePlan(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        EnterprisePlan ep = findEnterprisePlanByIdOrThrow(id);

        if (RoleConstants.COMPANY_EMPLOYEE.equals(principal.getRole())) {
            Long userEnterpriseId = getUserEnterpriseIdOrThrow(principal);
            if (!ep.getIdEnterprise().equals(userEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: colaboradores só podem cancelar planos da sua própria empresa");
            }
        } else if (!RoleConstants.ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para este perfil de usuário");
        }

        try {
            enterprisePlanRepository.delete(ep);
            enterprisePlanRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível cancelar o plano pois existem pagamentos vinculados a ele", ex);
        }
    }

    private EnterprisePlan findEnterprisePlanByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de empresa não encontrado para o ID: null");
        }
        return enterprisePlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de empresa não encontrado para o ID: " + id));
    }

    private Long resolveAndValidateEnterpriseId(Long requestedId, UserPrincipal principal, String actionDescription) {
        String role = principal.getRole();
        if (RoleConstants.ADM.equals(role)) {
            if (requestedId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID da empresa integradora é obrigatório para administradores");
            }
            return requestedId;
        }
        if (RoleConstants.COMPANY_EMPLOYEE.equals(role)) {
            Long userEnterpriseId = getUserEnterpriseIdOrThrow(principal);
            if (requestedId != null && !requestedId.equals(userEnterpriseId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: colaboradores não podem " + actionDescription + " outra empresa");
            }
            return userEnterpriseId;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: perfil de usuário sem permissão para gerenciar planos de empresas");
    }

    private Long getUserEnterpriseIdOrThrow(UserPrincipal principal) {
        return companyEmployeeRepository.findById(principal.getId())
                .map(CompanyEmployee::getIdEnterprise)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Vínculo de empresa do colaborador não encontrado"));
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }
}
