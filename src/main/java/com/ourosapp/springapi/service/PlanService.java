package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.plan.PlanRequestDTO;
import com.ourosapp.springapi.dto.plan.PlanResponseDTO;
import com.ourosapp.springapi.dto.plan.PlanUpdateDTO;
import com.ourosapp.springapi.entity.Plan;
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
import java.util.Optional;

/**
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link Plan}.
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    /**
     * Cadastra um novo Plano de Assinatura no sistema.
     * Operação restrita ao perfil ADM.
     */
    @Transactional
    public PlanResponseDTO createPlan(PlanRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);
        validateAdminPermission(principal, "cadastrar");

        if (planRepository.existsByTitleIgnoreCase(request.title())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um plano cadastrado com o título informado: " + request.title()
            );
        }

        Plan plan = Plan.builder()
                .title(request.title())
                .durationDays(request.durationDays())
                .description(request.description())
                .price(request.price())
                .build();

        try {
            Plan savedPlan = planRepository.save(plan);
            return PlanResponseDTO.fromEntity(savedPlan);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar plano",
                    ex
            );
        }
    }

    /**
     * Retorna a lista de todos os planos de assinatura disponíveis.
     * Permitido para perfis ADM e COMPANY_EMPLOYEE.
     */
    @Transactional(readOnly = true)
    public List<PlanResponseDTO> getAllPlans(UserPrincipal principal) {
        ensureAuthenticated(principal);
        validateReadPermission(principal);

        return planRepository.findAll()
                .stream()
                .map(PlanResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca os detalhes de um plano específico pelo seu identificador único.
     * Permitido para perfis ADM e COMPANY_EMPLOYEE.
     */
    @Transactional(readOnly = true)
    public PlanResponseDTO getPlanById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);
        validateReadPermission(principal);

        Plan plan = findPlanByIdOrThrow(id);
        return PlanResponseDTO.fromEntity(plan);
    }

    /**
     * Atualiza dados de um plano existente (PATCH ou PUT /plans/{id}).
     * Operação restrita ao perfil ADM.
     */
    @Transactional
    public PlanResponseDTO updatePlan(Long id, PlanUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);
        validateAdminPermission(principal, "atualizar");

        Plan plan = findPlanByIdOrThrow(id);

        if (!request.hasUpdates()) {
            return PlanResponseDTO.fromEntity(plan);
        }

        if (request.title() != null && !request.title().isBlank()) {
            Optional<Plan> existingPlanWithTitle = planRepository.findByTitleIgnoreCase(request.title());
            if (existingPlanWithTitle.isPresent() && !existingPlanWithTitle.get().getId().equals(id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Já existe outro plano cadastrado com o título informado: " + request.title()
                );
            }
            plan.setTitle(request.title());
        }

        if (request.durationDays() != null) {
            plan.setDurationDays(request.durationDays());
        }
        if (request.description() != null && !request.description().isBlank()) {
            plan.setDescription(request.description());
        }
        if (request.price() != null) {
            plan.setPrice(request.price());
        }

        try {
            Plan updatedPlan = planRepository.save(plan);
            return PlanResponseDTO.fromEntity(updatedPlan);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao atualizar plano",
                    ex
            );
        }
    }

    /**
     * Remove um plano de assinatura do sistema.
     * Operação restrita ao perfil ADM.
     */
    @Transactional
    public void deletePlan(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);
        validateAdminPermission(principal, "remover");

        Plan plan = findPlanByIdOrThrow(id);

        try {
            planRepository.delete(plan);
            planRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover o plano pois existem registros de adesão vinculados a ele",
                    ex
            );
        }
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    private void validateAdminPermission(UserPrincipal principal, String action) {
        if (!RoleConstants.ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso negado: apenas administradores podem " + action + " planos de assinatura"
            );
        }
    }

    private void validateReadPermission(UserPrincipal principal) {
        String role = principal.getRole();
        if (RoleConstants.ADM.equals(role) || RoleConstants.COMPANY_EMPLOYEE.equals(role)) {
            return;
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acesso negado: perfil de usuário sem permissão para consultar planos de assinatura"
        );
    }

    private Plan findPlanByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado para o ID: null");
        }
        return planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Plano não encontrado para o ID: " + id
                ));
    }
}
