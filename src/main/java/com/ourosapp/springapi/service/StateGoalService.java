package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.stategoal.RegionGoalRequestDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalRequestDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalUpdateDTO;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.repository.*;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelas regras de negócio e persistência de {@link StateGoal},
 * incluindo as tabelas de associação {@link FarmGoal}, {@link RegionGoal} e {@link StateGoalRegion}.
 */
@Service
@RequiredArgsConstructor
public class StateGoalService {

    private final StateGoalRepository stateGoalRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;
    private final FarmGoalRepository farmGoalRepository;
    private final RegionGoalRepository regionGoalRepository;
    private final StateGoalRegionRepository stateGoalRegionRepository;

    /**
     * Cadastra uma nova meta estadual para a fazenda e sincroniza as tabelas de associação.
     */
    @Transactional
    public StateGoalResponseDTO createStateGoal(StateGoalRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Long farmId = resolveFarmIdForCreation(request.idFarm(), principal);
        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal, "cadastrar metas estaduais nesta fazenda");

        if (request.dateEnd().isBefore(request.dateCreation())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data de término não pode ser anterior à data de criação"
            );
        }

        String region = request.region() != null && !request.region().isBlank() ? request.region() : farm.getRegion();

        StateGoal goal = StateGoal.builder()
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .status(request.status())
                .targetValue(request.targetValue())
                .dateCreation(request.dateCreation())
                .dateEnd(request.dateEnd())
                .idFarm(farm.getId())
                .build();

        try {
            StateGoal saved = stateGoalRepository.save(goal);

            // Sincroniza tabela de junção farm_goals
            if (!farmGoalRepository.existsByIdFarmAndIdGoal(farm.getId(), saved.getId())) {
                farmGoalRepository.save(FarmGoal.builder()
                        .idFarm(farm.getId())
                        .idGoal(saved.getId())
                        .build());
            }

            // Sincroniza tabelas regions_goals e state_goal_regions
            RegionGoal regionGoal = regionGoalRepository.save(RegionGoal.builder()
                    .region(region)
                    .idGoal(saved.getId())
                    .build());

            stateGoalRegionRepository.save(StateGoalRegion.builder()
                    .idGoal(saved.getId())
                    .idRegion(regionGoal.getId())
                    .build());

            return StateGoalResponseDTO.fromEntity(saved, region);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar meta estadual",
                    ex
            );
        }
    }

    /**
     * Lista todas as metas estaduais acessíveis ao usuário autenticado, com filtros opcionais por fazenda e região.
     */
    @Transactional(readOnly = true)
    public List<StateGoalResponseDTO> getStateGoalsForUser(Long farmIdFilter, String regionFilter, UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (farmIdFilter != null) {
            Farm farm = findFarmByIdOrThrow(farmIdFilter);
            validateFarmAccessPermission(farm, principal, "visualizar metas estaduais desta fazenda");

            if (regionFilter != null && !farm.getRegion().equalsIgnoreCase(regionFilter.trim())) {
                return List.of();
            }

            List<Long> linkedGoalIds = farmGoalRepository.findByIdFarm(farm.getId())
                    .stream()
                    .map(FarmGoal::getIdGoal)
                    .toList();

            List<StateGoal> directGoals = stateGoalRepository.findByIdFarm(farm.getId());
            List<StateGoal> junctionGoals = linkedGoalIds.isEmpty() ? List.of() : stateGoalRepository.findAllById(linkedGoalIds);

            Map<Long, StateGoal> combinedGoals = new java.util.LinkedHashMap<>();
            directGoals.forEach(g -> combinedGoals.put(g.getId(), g));
            junctionGoals.forEach(g -> combinedGoals.putIfAbsent(g.getId(), g));

            return combinedGoals.values().stream()
                    .map(goal -> StateGoalResponseDTO.fromEntity(goal, farm.getRegion()))
                    .toList();
        }

        String role = principal.getRole();
        if (ADM.equals(role)) {
            List<StateGoal> allGoals = stateGoalRepository.findAll();
            if (allGoals.isEmpty()) {
                return List.of();
            }

            Map<Long, Farm> farmMap = farmRepository.findAllById(
                    allGoals.stream().map(StateGoal::getIdFarm).distinct().toList()
            ).stream().collect(Collectors.toMap(Farm::getId, Function.identity()));

            return allGoals.stream()
                    .filter(goal -> {
                        Farm f = farmMap.get(goal.getIdFarm());
                        if (f == null) return false;
                        return regionFilter == null || f.getRegion().equalsIgnoreCase(regionFilter.trim());
                    })
                    .map(goal -> StateGoalResponseDTO.fromEntity(goal, farmMap.get(goal.getIdFarm()).getRegion()))
                    .toList();
        } else if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            List<Farm> farms = farmRepository.findAllByIdEnterprise(employee.getIdEnterprise());
            if (farms.isEmpty()) {
                return List.of();
            }

            Map<Long, Farm> farmMap = farms.stream()
                    .collect(Collectors.toMap(Farm::getId, Function.identity()));

            List<Long> farmIds = farms.stream().map(Farm::getId).toList();
            List<StateGoal> directGoals = stateGoalRepository.findByIdFarmIn(farmIds);

            List<FarmGoal> linkedFarmGoals = farmGoalRepository.findByIdFarmIn(farmIds);
            List<Long> linkedGoalIds = linkedFarmGoals.stream().map(FarmGoal::getIdGoal).toList();
            List<StateGoal> junctionGoals = linkedGoalIds.isEmpty() ? List.of() : stateGoalRepository.findAllById(linkedGoalIds);

            Map<Long, Long> goalToFarmId = new java.util.HashMap<>();
            directGoals.forEach(g -> goalToFarmId.put(g.getId(), g.getIdFarm()));
            linkedFarmGoals.forEach(fg -> goalToFarmId.putIfAbsent(fg.getIdGoal(), fg.getIdFarm()));

            Map<Long, StateGoal> combinedGoals = new java.util.LinkedHashMap<>();
            directGoals.forEach(g -> combinedGoals.put(g.getId(), g));
            junctionGoals.forEach(g -> combinedGoals.putIfAbsent(g.getId(), g));

            return combinedGoals.values().stream()
                    .filter(goal -> {
                        Long matchedFarmId = goalToFarmId.get(goal.getId());
                        Farm f = matchedFarmId != null ? farmMap.get(matchedFarmId) : farmMap.get(goal.getIdFarm());
                        if (f == null) return false;
                        return regionFilter == null || f.getRegion().equalsIgnoreCase(regionFilter.trim());
                    })
                    .map(goal -> {
                        Long matchedFarmId = goalToFarmId.get(goal.getId());
                        Farm f = matchedFarmId != null ? farmMap.get(matchedFarmId) : farmMap.get(goal.getIdFarm());
                        String region = f != null ? f.getRegion() : null;
                        return StateGoalResponseDTO.fromEntity(goal, region);
                    })
                    .toList();
        } else if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                return List.of();
            }
            Farm farm = findFarmByIdOrThrow(owner.getIdFarm());
            if (regionFilter != null && !farm.getRegion().equalsIgnoreCase(regionFilter.trim())) {
                return List.of();
            }

            List<Long> linkedGoalIds = farmGoalRepository.findByIdFarm(farm.getId())
                    .stream()
                    .map(FarmGoal::getIdGoal)
                    .toList();

            List<StateGoal> directGoals = stateGoalRepository.findByIdFarm(farm.getId());
            List<StateGoal> junctionGoals = linkedGoalIds.isEmpty() ? List.of() : stateGoalRepository.findAllById(linkedGoalIds);

            Map<Long, StateGoal> combinedGoals = new java.util.LinkedHashMap<>();
            directGoals.forEach(g -> combinedGoals.put(g.getId(), g));
            junctionGoals.forEach(g -> combinedGoals.putIfAbsent(g.getId(), g));

            return combinedGoals.values().stream()
                    .map(goal -> StateGoalResponseDTO.fromEntity(goal, farm.getRegion()))
                    .toList();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar metas estaduais"
            );
        }
    }

    /**
     * Busca os detalhes de uma meta estadual pelo seu ID.
     */
    @Transactional(readOnly = true)
    public StateGoalResponseDTO getStateGoalById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "visualizar esta meta estadual");

        return StateGoalResponseDTO.fromEntity(goal, farm.getRegion());
    }

    /**
     * Atualiza parcialmente dados de uma meta estadual existente.
     */
    @Transactional
    public StateGoalResponseDTO updateStateGoal(Long id, StateGoalUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "alterar metas estaduais desta fazenda");

        if (!request.hasUpdates()) {
            return StateGoalResponseDTO.fromEntity(goal, farm.getRegion());
        }

        if (request.status() != null && !request.status().isBlank()) {
            goal.setStatus(request.status());
        }
        if (request.dateEnd() != null) {
            if (request.dateEnd().isBefore(goal.getDateCreation())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A data de término não pode ser anterior à data de criação"
                );
            }
            goal.setDateEnd(request.dateEnd());
        }
        if (request.targetValue() != null) {
            goal.setTargetValue(request.targetValue());
        }

        try {
            StateGoal updated = stateGoalRepository.save(goal);
            return StateGoalResponseDTO.fromEntity(updated, farm.getRegion());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao atualizar meta estadual",
                    ex
            );
        }
    }

    /**
     * Remove uma meta estadual e suas associações pelo seu ID.
     */
    @Transactional
    public void deleteStateGoal(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "remover meta estadual desta fazenda");

        stateGoalRegionRepository.deleteByIdGoal(goal.getId());
        regionGoalRepository.deleteByIdGoal(goal.getId());
        farmGoalRepository.deleteByIdGoal(goal.getId());

        stateGoalRepository.delete(goal);
    }

    /**
     * Vincula uma fazenda adicional à meta estadual (tabela farm_goals).
     */
    @Transactional
    public void addFarmToStateGoal(Long goalId, Long farmId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(goalId);
        Farm primaryFarm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(primaryFarm, principal, "gerenciar esta meta estadual");

        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal, "vincular fazenda a esta meta estadual");

        if (farmGoalRepository.existsByIdFarmAndIdGoal(farm.getId(), goal.getId())) {
            return; // Idempotente
        }

        farmGoalRepository.save(FarmGoal.builder()
                .idFarm(farm.getId())
                .idGoal(goal.getId())
                .build());
    }

    /**
     * Desvincula uma fazenda da meta estadual (tabela farm_goals).
     */
    @Transactional
    public void removeFarmFromStateGoal(Long goalId, Long farmId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(goalId);
        Farm primaryFarm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(primaryFarm, principal, "gerenciar esta meta estadual");

        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal, "desvincular fazenda desta meta estadual");

        if (Objects.equals(goal.getIdFarm(), farm.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não é permitido desvincular a fazenda principal da meta estadual. Para remover a meta, utilize o endpoint de exclusão."
            );
        }

        farmGoalRepository.deleteByIdFarmAndIdGoal(farm.getId(), goal.getId());
    }

    /**
     * Lista todas as fazendas vinculadas à meta estadual.
     */
    @Transactional(readOnly = true)
    public List<FarmResponseDTO> getFarmsByStateGoalId(Long goalId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(goalId);
        Farm primaryFarm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(primaryFarm, principal, "visualizar fazendas vinculadas a esta meta estadual");

        List<FarmGoal> farmGoals = farmGoalRepository.findByIdGoal(goal.getId());
        List<Long> farmIds = farmGoals.stream().map(FarmGoal::getIdFarm).toList();
        if (farmIds.isEmpty()) {
            return List.of(FarmResponseDTO.fromEntity(primaryFarm));
        }

        return farmRepository.findAllById(farmIds)
                .stream()
                .map(FarmResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Adiciona uma região à meta estadual (tabelas regions_goals e state_goal_regions).
     */
    @Transactional
    public void addRegionToStateGoal(Long goalId, RegionGoalRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da região não pode ser nulo");
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(goalId);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "adicionar região a esta meta estadual");

        if (regionGoalRepository.existsByRegionAndIdGoal(request.region(), goal.getId())) {
            return; // Idempotente
        }

        RegionGoal regionGoal = regionGoalRepository.save(RegionGoal.builder()
                .region(request.region())
                .idGoal(goal.getId())
                .build());

        stateGoalRegionRepository.save(StateGoalRegion.builder()
                .idGoal(goal.getId())
                .idRegion(regionGoal.getId())
                .build());
    }

    /**
     * Remove uma região da meta estadual.
     */
    @Transactional
    public void removeRegionFromStateGoal(Long goalId, String region, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(goalId);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "remover região desta meta estadual");

        regionGoalRepository.findByRegionAndIdGoal(region, goal.getId()).ifPresent(rg -> {
            stateGoalRegionRepository.deleteByIdGoalAndIdRegion(goal.getId(), rg.getId());
            regionGoalRepository.delete(rg);
        });
    }

    /**
     * Lista todas as regiões vinculadas à meta estadual.
     */
    @Transactional(readOnly = true)
    public List<String> getRegionsByStateGoalId(Long goalId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(goalId);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "visualizar regiões vinculadas a esta meta estadual");

        return regionGoalRepository.findByIdGoal(goal.getId())
                .stream()
                .map(RegionGoal::getRegion)
                .distinct()
                .toList();
    }

    private void validateFarmAccessPermission(Farm farm, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action
                );
            }
            return;
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(farm.getId(), owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para acessar esta fazenda"
        );
    }

    private Long resolveFarmIdForCreation(Long idFarm, UserPrincipal principal) {
        if (idFarm != null) {
            return idFarm;
        }

        if (FARM_OWNER.equals(principal.getRole())) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Produtor rural logado não possui fazenda vinculada para cadastrar meta estadual"
                );
            }
            return owner.getIdFarm();
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "O ID da fazenda é obrigatório para administradores e funcionários da empresa"
        );
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    private CompanyEmployee getCompanyEmployeeOrThrow(Long id) {
        return companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário logado não encontrado para o ID: " + id
                ));
    }

    private FarmOwner getFarmOwnerOrThrow(Long id) {
        return farmOwnerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produtor rural logado não encontrado para o ID: " + id
                ));
    }

    private Farm findFarmByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: null");
        }
        return farmRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + id
                ));
    }

    private StateGoal findStateGoalByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta estadual não encontrada para o ID: null");
        }
        return stateGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Meta estadual não encontrada para o ID: " + id
                ));
    }
}
