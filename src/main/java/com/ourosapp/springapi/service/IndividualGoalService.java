package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.individualgoal.IndividualGoalRequestDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalResponseDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.IndividualGoal;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.IndividualGoalRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Serviço responsável pelas regras de negócio e operações de persistência de {@link IndividualGoal}.
 */
@Service
@RequiredArgsConstructor
public class IndividualGoalService {

    private final IndividualGoalRepository individualGoalRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Cadastra uma nova meta individual para a fazenda.
     *
     * @param request   payload da requisição com os dados da meta
     * @param principal dados do usuário autenticado via token JWT
     * @return DTO com os dados da meta cadastrada
     * @throws ResponseStatusException HTTP 400 se o ID da fazenda for inválido ou ausente
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para a fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda não for encontrada
     * @throws ResponseStatusException HTTP 409 se houver violação de integridade de dados
     */
    @Transactional
    public IndividualGoalResponseDTO createIndividualGoal(IndividualGoalRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Long farmId = resolveFarmIdForCreation(request.idFarm(), principal);
        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal, "cadastrar metas individuais nesta fazenda");

        IndividualGoal goal = IndividualGoal.builder()
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .status(request.status())
                .targetValue(request.targetValue())
                .idFarm(farm.getId())
                .build();

        IndividualGoal saved = individualGoalRepository.save(goal);
        return IndividualGoalResponseDTO.fromEntity(saved);
    }

    /**
     * Lista todas as metas individuais acessíveis ao usuário autenticado, com filtro opcional por fazenda.
     *
     * @param farmIdFilter ID opcional da fazenda para filtragem
     * @param principal    dados do usuário autenticado via token JWT
     * @return lista de metas individuais encontradas
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver acesso à fazenda filtrada
     * @throws ResponseStatusException HTTP 404 se a fazenda filtrada não for encontrada
     */
    @Transactional(readOnly = true)
    public List<IndividualGoalResponseDTO> getIndividualGoalsForUser(Long farmIdFilter, UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (farmIdFilter != null) {
            Farm farm = findFarmByIdOrThrow(farmIdFilter);
            validateFarmAccessPermission(farm, principal, "visualizar metas individuais desta fazenda");
            return individualGoalRepository.findByIdFarm(farm.getId())
                    .stream()
                    .map(IndividualGoalResponseDTO::fromEntity)
                    .toList();
        }

        return switch (principal.getRole() != null ? principal.getRole() : "") {
            case ADM -> individualGoalRepository.findAll()
                    .stream()
                    .map(IndividualGoalResponseDTO::fromEntity)
                    .toList();
            case COMPANY_EMPLOYEE -> {
                CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
                List<Long> farmIds = farmRepository.findAllByIdEnterprise(employee.getIdEnterprise())
                        .stream()
                        .map(Farm::getId)
                        .toList();
                yield farmIds.isEmpty()
                        ? List.of()
                        : individualGoalRepository.findByIdFarmIn(farmIds)
                                .stream()
                                .map(IndividualGoalResponseDTO::fromEntity)
                                .toList();
            }
            case FARM_OWNER -> {
                FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
                yield (owner.getIdFarm() == null)
                        ? List.of()
                        : individualGoalRepository.findByIdFarm(owner.getIdFarm())
                                .stream()
                                .map(IndividualGoalResponseDTO::fromEntity)
                                .toList();
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar metas individuais"
            );
        };
    }

    /**
     * Busca os detalhes de uma meta individual pelo seu ID.
     *
     * @param id        identificador único da meta individual
     * @param principal dados do usuário autenticado via token JWT
     * @return DTO com os detalhes da meta individual
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se não tiver permissão para a fazenda vinculada
     * @throws ResponseStatusException HTTP 404 se a meta ou a fazenda não forem encontradas
     */
    @Transactional(readOnly = true)
    public IndividualGoalResponseDTO getIndividualGoalById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        IndividualGoal goal = findIndividualGoalByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "visualizar esta meta individual");

        return IndividualGoalResponseDTO.fromEntity(goal);
    }

    /**
     * Atualiza parcialmente dados de uma meta individual existente.
     *
     * @param id        identificador único da meta a ser atualizada
     * @param request   payload com os dados opcionais de atualização
     * @param principal dados do usuário autenticado via token JWT
     * @return DTO com os dados atualizados da meta
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se não tiver permissão para a fazenda vinculada
     * @throws ResponseStatusException HTTP 404 se a meta ou a fazenda não forem encontradas
     * @throws ResponseStatusException HTTP 409 se houver conflito de integridade
     */
    @Transactional
    public IndividualGoalResponseDTO updateIndividualGoal(Long id, IndividualGoalUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        IndividualGoal goal = findIndividualGoalByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "alterar metas individuais desta fazenda");

        if (!request.hasUpdates()) {
            return IndividualGoalResponseDTO.fromEntity(goal);
        }

        if (request.title() != null && !request.title().isBlank()) {
            goal.setTitle(request.title());
        }
        if (request.description() != null) {
            goal.setDescription(request.description());
        }
        if (request.status() != null && !request.status().isBlank()) {
            goal.setStatus(request.status());
        }
        if (request.targetValue() != null) {
            goal.setTargetValue(request.targetValue());
        }

        IndividualGoal updated = individualGoalRepository.save(goal);
        return IndividualGoalResponseDTO.fromEntity(updated);
    }

    /**
     * Remove uma meta individual pelo seu ID.
     *
     * @param id        identificador único da meta individual a ser removida
     * @param principal dados do usuário autenticado via token JWT
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se não tiver permissão para a fazenda vinculada
     * @throws ResponseStatusException HTTP 404 se a meta não for encontrada
     */
    @Transactional
    public void deleteIndividualGoal(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        IndividualGoal goal = findIndividualGoalByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(goal.getIdFarm());
        validateFarmAccessPermission(farm, principal, "remover meta individual desta fazenda");

        individualGoalRepository.delete(goal);
    }

    /**
     * Valida se o usuário autenticado tem permissão para acessar/manipular metas da fazenda especificada.
     */
    private void validateFarmAccessPermission(Farm farm, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        boolean authorized = switch (principal.getRole() != null ? principal.getRole() : "") {
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

        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para " + action);
        }
    }

    /**
     * Resolve o identificador da fazenda para cadastro de meta individual.
     */
    private Long resolveFarmIdForCreation(Long idFarm, UserPrincipal principal) {
        if (idFarm != null) {
            return idFarm;
        }

        if (FARM_OWNER.equals(principal.getRole())) {
            Long ownerFarmId = getFarmOwnerOrThrow(principal.getId()).getIdFarm();
            if (ownerFarmId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Produtor rural logado não possui fazenda vinculada para cadastrar meta individual"
                );
            }
            return ownerFarmId;
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
        return Optional.ofNullable(farmId)
                .flatMap(farmRepository::findById)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + farmId
                ));
    }

    private IndividualGoal findIndividualGoalByIdOrThrow(Long goalId) {
        return Optional.ofNullable(goalId)
                .flatMap(individualGoalRepository::findById)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Meta individual não encontrada para o ID: " + goalId
                ));
    }
}
