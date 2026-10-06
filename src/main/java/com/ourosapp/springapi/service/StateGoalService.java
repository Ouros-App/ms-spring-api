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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

        if (request.dateEnd().isBefore(request.dateCreation())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data de término não pode ser anterior à data de criação"
            );
        }

        Long farmId = resolveFarmIdForCreation(request.idFarm(), principal);
        Farm farm = farmId != null ? findFarmByIdOrThrow(farmId) : null;
        if (farm != null) {
            validateFarmAccessPermission(farm, principal, "cadastrar metas estaduais nesta fazenda");
        }

        String region = request.region() != null && !request.region().isBlank()
                ? request.region()
                : (farm != null ? farm.getRegion() : null);
        Integer farmIdInt = farm != null && farm.getId() != null ? farm.getId().intValue() : null;
        LocalDateTime dateCreation = request.dateCreation() != null ? request.dateCreation().atStartOfDay() : null;
        LocalDateTime dateEnd = request.dateEnd() != null ? request.dateEnd().atStartOfDay() : null;

        Integer generatedGoalIdInt = stateGoalRepository.callCreateStateGoal(
                request.title(),
                request.description(),
                request.type(),
                request.status(),
                request.targetValue(),
                dateCreation,
                dateEnd,
                farmIdInt,
                region
        );
        Long generatedGoalId = generatedGoalIdInt != null ? generatedGoalIdInt.longValue() : null;

        StateGoal saved = StateGoal.builder()
                .id(generatedGoalId)
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .status(request.status())
                .targetValue(request.targetValue())
                .dateCreation(request.dateCreation())
                .dateEnd(request.dateEnd())
                .build();

        return StateGoalResponseDTO.fromEntity(saved, region);
    }

    /**
     * Cadastra uma nova meta estadual utilizando a stored procedure PostgreSQL 'create_state_goal'.
     * Substitui a persistência encadeada de 4 entidades por uma execução transacional atômica no banco de dados.
     *
     * @param request   dados da meta estadual
     * @param principal dados do usuário autenticado no JWT
     * @return DTO com os dados da meta estadual criada e ID gerado
     */
    @Transactional
    public StateGoalResponseDTO createStateGoalViaProcedure(StateGoalRequestDTO request, UserPrincipal principal) {
        return createStateGoal(request, principal);
    }

    /**
     * Sobrecarga de compatibilidade para chamada com idRegion numérico legado.
     *
     * @param request   dados da meta estadual
     * @param idRegion  identificador numérico da região (ignorado em favor da região textual)
     * @param principal dados do usuário autenticado no JWT
     * @return DTO com os dados da meta estadual criada e ID gerado
     */
    @Transactional
    public StateGoalResponseDTO createStateGoalViaProcedure(StateGoalRequestDTO request, Long idRegion, UserPrincipal principal) {
        return createStateGoal(request, principal);
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
            return getGoalsForSingleFarm(farm, regionFilter);
        }

        String role = principal.getRole();
        if (ADM.equals(role)) {
            List<StateGoal> allGoals = stateGoalRepository.findAll();
            if (allGoals.isEmpty()) {
                return List.of();
            }

            List<FarmGoal> allFarmGoals = farmGoalRepository.findAll();
            Map<Long, Long> goalToFarmId = allFarmGoals.stream()
                    .collect(Collectors.toMap(FarmGoal::getIdGoal, FarmGoal::getIdFarm, (existing, replacement) -> existing));

            Map<Long, Farm> farmMap = farmRepository.findAllById(
                    goalToFarmId.values().stream().distinct().toList()
            ).stream().collect(Collectors.toMap(Farm::getId, Function.identity()));

            return allGoals.stream()
                    .flatMap(goal -> {
                        Long farmId = goalToFarmId.get(goal.getId());
                        Farm f = farmId != null ? farmMap.get(farmId) : null;
                        String defaultRegion = f != null ? f.getRegion() : null;
                        return toResponseIfRegionMatches(goal, defaultRegion, regionFilter).stream();
                    })
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
            List<FarmGoal> linkedFarmGoals = farmGoalRepository.findByIdFarmIn(farmIds);
            List<Long> linkedGoalIds = linkedFarmGoals.stream().map(FarmGoal::getIdGoal).distinct().toList();
            List<StateGoal> goals = linkedGoalIds.isEmpty() ? List.of() : stateGoalRepository.findAllById(linkedGoalIds);

            Map<Long, Long> goalToFarmId = linkedFarmGoals.stream()
                    .collect(Collectors.toMap(FarmGoal::getIdGoal, FarmGoal::getIdFarm, (existing, replacement) -> existing));

            return goals.stream()
                    .flatMap(goal -> {
                        Long matchedFarmId = goalToFarmId.get(goal.getId());
                        Farm f = matchedFarmId != null ? farmMap.get(matchedFarmId) : null;
                        String defaultRegion = f != null ? f.getRegion() : null;
                        return toResponseIfRegionMatches(goal, defaultRegion, regionFilter).stream();
                    })
                    .toList();
        } else if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                return List.of();
            }
            Farm farm = findFarmByIdOrThrow(owner.getIdFarm());
            return getGoalsForSingleFarm(farm, regionFilter);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar metas estaduais"
            );
        }
    }

    private List<StateGoalResponseDTO> getGoalsForSingleFarm(Farm farm, String regionFilter) {
        List<Long> linkedGoalIds = farmGoalRepository.findByIdFarm(farm.getId())
                .stream()
                .map(FarmGoal::getIdGoal)
                .distinct()
                .toList();

        List<StateGoal> goals = linkedGoalIds.isEmpty() ? List.of() : stateGoalRepository.findAllById(linkedGoalIds);

        return goals.stream()
                .flatMap(goal -> toResponseIfRegionMatches(goal, farm.getRegion(), regionFilter).stream())
                .toList();
    }

    private Optional<StateGoalResponseDTO> toResponseIfRegionMatches(
            StateGoal goal,
            String defaultRegion,
            String regionFilter
    ) {
        List<RegionGoal> regionGoals = regionGoalRepository.findByIdGoal(goal.getId());
        boolean matchesFilter = regionFilter == null || (regionGoals.isEmpty()
                ? regionsMatch(defaultRegion, regionFilter)
                : regionGoals.stream()
                        .map(RegionGoal::getRegion)
                        .anyMatch(region -> regionsMatch(region, regionFilter)));

        if (!matchesFilter) {
            return Optional.empty();
        }

        String primaryRegion = regionGoals.stream()
                .map(RegionGoal::getRegion)
                .findFirst()
                .orElse(defaultRegion);
        return Optional.of(StateGoalResponseDTO.fromEntity(goal, primaryRegion));
    }

    private boolean regionsMatch(String region, String regionFilter) {
        return region != null && region.equalsIgnoreCase(regionFilter.trim());
    }

    /**
     * Busca os detalhes de uma meta estadual pelo seu ID.
     */
    @Transactional(readOnly = true)
    public StateGoalResponseDTO getStateGoalById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(id);
        Farm primaryFarm = findPrimaryFarmForGoal(goal.getId());
        validateStateGoalReadPermission(goal, primaryFarm, principal, "visualizar esta meta estadual");

        String region = regionGoalRepository.findByIdGoal(goal.getId())
                .stream()
                .map(RegionGoal::getRegion)
                .findFirst()
                .orElse(primaryFarm != null ? primaryFarm.getRegion() : null);

        return StateGoalResponseDTO.fromEntity(goal, region);
    }

    /**
     * Atualiza parcialmente dados de uma meta estadual existente.
     */
    @Transactional
    public StateGoalResponseDTO updateStateGoal(Long id, StateGoalUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(id);
        Farm primaryFarm = findPrimaryFarmForGoal(goal.getId());
        if (primaryFarm != null) {
            validateFarmAccessPermission(primaryFarm, principal, "alterar metas estaduais desta fazenda");
        } else if (!ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para alterar metas estaduais");
        }

        String region = regionGoalRepository.findByIdGoal(goal.getId())
                .stream()
                .map(RegionGoal::getRegion)
                .findFirst()
                .orElse(primaryFarm != null ? primaryFarm.getRegion() : null);

        if (!request.hasUpdates()) {
            return StateGoalResponseDTO.fromEntity(goal, region);
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

        StateGoal updated = stateGoalRepository.save(goal);
        return StateGoalResponseDTO.fromEntity(updated, region);
    }

    /**
     * Remove uma meta estadual e suas associações pelo seu ID.
     */
    @Transactional
    public void deleteStateGoal(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        StateGoal goal = findStateGoalByIdOrThrow(id);
        Farm primaryFarm = findPrimaryFarmForGoal(goal.getId());
        if (primaryFarm != null) {
            validateFarmAccessPermission(primaryFarm, principal, "remover meta estadual desta fazenda");
        } else if (!ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para remover meta estadual");
        }

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
        Farm primaryFarm = findPrimaryFarmForGoal(goal.getId());
        if (primaryFarm != null) {
            validateFarmAccessPermission(primaryFarm, principal, "gerenciar esta meta estadual");
        } else if (!ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para gerenciar esta meta estadual");
        }

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
        Farm primaryFarm = findPrimaryFarmForGoal(goal.getId());
        if (primaryFarm != null) {
            validateFarmAccessPermission(primaryFarm, principal, "gerenciar esta meta estadual");
        } else if (!ADM.equals(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para gerenciar esta meta estadual");
        }

        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal, "desvincular fazenda desta meta estadual");

        if (primaryFarm != null && Objects.equals(primaryFarm.getId(), farm.getId())) {
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
        Farm primaryFarm = findPrimaryFarmForGoal(goal.getId());
        validateStateGoalReadPermission(goal, primaryFarm, principal, "visualizar fazendas vinculadas a esta meta estadual");

        List<FarmGoal> farmGoals = farmGoalRepository.findByIdGoal(goal.getId());
        List<Long> farmIds = farmGoals.stream().map(FarmGoal::getIdFarm).toList();
        if (farmIds.isEmpty()) {
            return primaryFarm != null ? List.of(FarmResponseDTO.fromEntity(primaryFarm)) : List.of();
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
        Farm farm = findPrimaryFarmForGoal(goal.getId());
        if (farm != null) {
            validateFarmAccessPermission(farm, principal, "adicionar região a esta meta estadual");
        }

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
        Farm farm = findPrimaryFarmForGoal(goal.getId());
        if (farm != null) {
            validateFarmAccessPermission(farm, principal, "remover região desta meta estadual");
        }

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
        Farm farm = findPrimaryFarmForGoal(goal.getId());
        validateStateGoalReadPermission(goal, farm, principal, "visualizar regiões vinculadas a esta meta estadual");

        return regionGoalRepository.findByIdGoal(goal.getId())
                .stream()
                .map(RegionGoal::getRegion)
                .distinct()
                .toList();
    }

    private Farm findPrimaryFarmForGoal(Long goalId) {
        List<FarmGoal> farmGoals = farmGoalRepository.findByIdGoal(goalId);
        if (farmGoals.isEmpty()) {
            return null;
        }
        return farmRepository.findById(farmGoals.get(0).getIdFarm()).orElse(null);
    }

    private void validateStateGoalReadPermission(StateGoal goal, Farm primaryFarm, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        boolean isAuthorized = switch (principal.getRole()) {
            case ADM -> true;
            case COMPANY_EMPLOYEE -> hasEnterpriseAccessToStateGoal(
                    goal,
                    primaryFarm,
                    getCompanyEmployeeOrThrow(principal.getId()).getIdEnterprise()
            );
            case FARM_OWNER -> {
                FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
                yield (primaryFarm != null && Objects.equals(primaryFarm.getId(), owner.getIdFarm()))
                        || (owner.getIdFarm() != null && farmGoalRepository.existsByIdFarmAndIdGoal(owner.getIdFarm(), goal.getId()));
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para acessar esta meta estadual"
            );
        };

        if (!isAuthorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para " + action);
        }
    }

    private boolean hasEnterpriseAccessToStateGoal(StateGoal goal, Farm primaryFarm, Long enterpriseId) {
        if (primaryFarm != null && Objects.equals(primaryFarm.getIdEnterprise(), enterpriseId)) {
            return true;
        }

        List<Long> linkedFarmIds = farmGoalRepository.findByIdGoal(goal.getId())
                .stream()
                .map(FarmGoal::getIdFarm)
                .filter(idFarm -> primaryFarm == null || !Objects.equals(idFarm, primaryFarm.getId()))
                .distinct()
                .toList();

        return !linkedFarmIds.isEmpty() && farmRepository.findAllById(linkedFarmIds)
                .stream()
                .anyMatch(farm -> Objects.equals(farm.getIdEnterprise(), enterpriseId));
    }

    private void validateFarmAccessPermission(Farm farm, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        boolean isAuthorized = switch (principal.getRole()) {
            case ADM -> true;
            case COMPANY_EMPLOYEE -> Objects.equals(
                    farm.getIdEnterprise(),
                    getCompanyEmployeeOrThrow(principal.getId()).getIdEnterprise()
            );
            case FARM_OWNER -> Objects.equals(
                    farm.getId(),
                    getFarmOwnerOrThrow(principal.getId()).getIdFarm()
            );
            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para acessar esta fazenda"
            );
        };

        if (!isAuthorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para " + action);
        }
    }

    private Long resolveFarmIdForCreation(Long idFarm, UserPrincipal principal) {
        if (idFarm != null) {
            return idFarm;
        }

        if (ADM.equals(principal.getRole()) || COMPANY_EMPLOYEE.equals(principal.getRole())) {
            return null;
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
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para cadastrar metas estaduais"
        );
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    private CompanyEmployee getCompanyEmployeeOrThrow(Long employeeId) {
        return companyEmployeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário logado não encontrado para o ID: " + employeeId
                ));
    }

    private FarmOwner getFarmOwnerOrThrow(Long ownerId) {
        return farmOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produtor rural logado não encontrado para o ID: " + ownerId
                ));
    }

    private Farm findFarmByIdOrThrow(Long farmId) {
        if (farmId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: null");
        }
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + farmId
                ));
    }

    private StateGoal findStateGoalByIdOrThrow(Long goalId) {
        if (goalId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta estadual não encontrada para o ID: null");
        }
        return stateGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Meta estadual não encontrada para o ID: " + goalId
                ));
    }
}
