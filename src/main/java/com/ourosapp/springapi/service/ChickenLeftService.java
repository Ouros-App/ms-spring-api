package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.chickenleft.ChickenLeftRequestDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftResponseDTO;
import com.ourosapp.springapi.dto.chickenleft.ChickenLeftUpdateDTO;
import com.ourosapp.springapi.entity.ChickenLeft;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.ChickenLeftRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
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
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link ChickenLeft}.
 */
@Service
@RequiredArgsConstructor
public class ChickenLeftService {

    private final ChickenLeftRepository chickenLeftRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Cadastra um novo Registro de Saída de Aves vinculado a uma Fazenda.
     * Atualiza atomicamente o saldo de aves atual (chickens_now) na fazenda.
     *
     * @param request   payload da requisição contendo os dados da saída de aves
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados do registro de saída cadastrado
     * @throws ResponseStatusException HTTP 400 se os dados forem inválidos ou se o saldo de aves for insuficiente
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão para cadastrar na fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda informada não existir
     * @throws ResponseStatusException HTTP 409 se houver conflito de integridade de dados
     */
    @Transactional
    public ChickenLeftResponseDTO createChickenLeft(ChickenLeftRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Long resolvedFarmId = resolveAndValidateFarmForCreation(request.idFarm(), principal);
        Farm farm = findFarmByIdOrThrow(resolvedFarmId);

        int currentChickens = farm.getChickensNow() != null ? farm.getChickensNow() : 0;
        if (currentChickens < request.chickensCount()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "A quantidade de saída de aves (%d) é maior que o saldo de aves atual na fazenda (%d)",
                            request.chickensCount(),
                            currentChickens
                    )
            );
        }

        farm.setChickensNow(currentChickens - request.chickensCount());
        farmRepository.save(farm);

        ChickenLeft chickenLeft = ChickenLeft.builder()
                .chickensCount(request.chickensCount())
                .exitDate(request.exitDate())
                .idFarm(resolvedFarmId)
                .build();

        try {
            ChickenLeft saved = chickenLeftRepository.save(chickenLeft);
            return ChickenLeftResponseDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar saída de aves",
                    ex
            );
        }
    }

    /**
     * Retorna a lista de registros de saída de aves acessíveis ao usuário autenticado.
     * Permite filtrar por um ID de fazenda específico se o usuário tiver permissão.
     *
     * @param farmId    identificador opcional da fazenda para filtro
     * @param principal dados do usuário logado extraídos do token JWT
     * @return lista de DTOs com os registros de saída de aves
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão
     * @throws ResponseStatusException HTTP 404 se a fazenda informada não existir
     */
    @Transactional(readOnly = true)
    public List<ChickenLeftResponseDTO> getChickenLeftsForUser(Long farmId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (FARM_OWNER.equals(role)) {
            return getChickenLeftsForFarmOwner(farmId, principal.getId());
        } else if (COMPANY_EMPLOYEE.equals(role)) {
            return getChickenLeftsForCompanyEmployee(farmId, principal.getId());
        } else if (ADM.equals(role)) {
            return getChickenLeftsForAdm(farmId);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar registros de saída de aves"
            );
        }
    }

    private List<ChickenLeftResponseDTO> getChickenLeftsForFarmOwner(Long farmId, Long userId) {
        FarmOwner owner = getFarmOwnerOrThrow(userId);
        if (owner.getIdFarm() == null) {
            return List.of();
        }
        if (farmId != null && !Objects.equals(farmId, owner.getIdFarm())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso negado aos registros de saída de aves de outra fazenda"
            );
        }
        return chickenLeftRepository.findAllByIdFarm(owner.getIdFarm())
                .stream()
                .map(ChickenLeftResponseDTO::fromEntity)
                .toList();
    }

    private List<ChickenLeftResponseDTO> getChickenLeftsForCompanyEmployee(Long farmId, Long userId) {
        CompanyEmployee employee = getCompanyEmployeeOrThrow(userId);
        if (farmId != null) {
            Farm farm = findFarmByIdOrThrow(farmId);
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado aos registros de saída de aves de fazenda vinculada a outra empresa"
                );
            }
            return chickenLeftRepository.findAllByIdFarm(farmId)
                    .stream()
                    .map(ChickenLeftResponseDTO::fromEntity)
                    .toList();
        }
        List<Farm> enterpriseFarms = farmRepository.findAllByIdEnterprise(employee.getIdEnterprise());
        List<Long> farmIds = enterpriseFarms.stream().map(Farm::getId).toList();
        if (farmIds.isEmpty()) {
            return List.of();
        }
        return chickenLeftRepository.findAllByIdFarmIn(farmIds)
                .stream()
                .map(ChickenLeftResponseDTO::fromEntity)
                .toList();
    }

    private List<ChickenLeftResponseDTO> getChickenLeftsForAdm(Long farmId) {
        if (farmId != null) {
            findFarmByIdOrThrow(farmId);
            return chickenLeftRepository.findAllByIdFarm(farmId)
                    .stream()
                    .map(ChickenLeftResponseDTO::fromEntity)
                    .toList();
        }
        return chickenLeftRepository.findAll()
                .stream()
                .map(ChickenLeftResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca os detalhes de um registro de saída de aves específico pelo seu ID.
     * Valida se o usuário autenticado possui permissão de leitura na fazenda vinculada.
     *
     * @param id        identificador único do registro de saída de aves
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os detalhes do registro
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão
     * @throws ResponseStatusException HTTP 404 se o registro não for encontrado
     */
    @Transactional(readOnly = true)
    public ChickenLeftResponseDTO getChickenLeftById(Long id, UserPrincipal principal) {
        ChickenLeft chickenLeft = findChickenLeftByIdOrThrow(id);
        validateChickenLeftAccess(chickenLeft, principal);
        return ChickenLeftResponseDTO.fromEntity(chickenLeft);
    }

    /**
     * Atualiza parcialmente um registro de saída de aves existente (PATCH /chicken-left/{id}).
     * Ajusta o saldo de aves na fazenda de acordo com o delta da quantidade.
     *
     * @param id        identificador único do registro
     * @param request   payload com os campos parciais a serem atualizados
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados atualizados
     * @throws ResponseStatusException HTTP 400 se o novo saldo de aves for insuficiente
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão
     * @throws ResponseStatusException HTTP 404 se o registro não for encontrado
     */
    @Transactional
    public ChickenLeftResponseDTO updateChickenLeft(Long id, ChickenLeftUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        ChickenLeft chickenLeft = findChickenLeftByIdOrThrow(id);
        validateChickenLeftMutation(chickenLeft, principal, "alterar");

        if (!request.hasUpdates()) {
            return ChickenLeftResponseDTO.fromEntity(chickenLeft);
        }

        if (request.chickensCount() != null && !request.chickensCount().equals(chickenLeft.getChickensCount())) {
            int delta = request.chickensCount() - chickenLeft.getChickensCount();
            Farm farm = findFarmByIdOrThrow(chickenLeft.getIdFarm());
            int currentChickens = farm.getChickensNow() != null ? farm.getChickensNow() : 0;

            if (delta > 0 && currentChickens < delta) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "A quantidade adicional de aves de saída (%d) é maior que o saldo de aves atual na fazenda (%d)",
                                delta,
                                currentChickens
                        )
                );
            }

            farm.setChickensNow(currentChickens - delta);
            farmRepository.save(farm);
            chickenLeft.setChickensCount(request.chickensCount());
        }

        if (request.exitDate() != null) {
            chickenLeft.setExitDate(request.exitDate());
        }

        ChickenLeft updated = chickenLeftRepository.save(chickenLeft);
        return ChickenLeftResponseDTO.fromEntity(updated);
    }

    /**
     * Remove um registro de saída de aves do sistema e estorna a quantidade ao saldo da fazenda.
     *
     * @param id        identificador único do registro a ser removido
     * @param principal dados do usuário logado extraídos do token JWT
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão
     * @throws ResponseStatusException HTTP 404 se o registro não for encontrado
     * @throws ResponseStatusException HTTP 409 se existirem registros dependentes vinculados
     */
    @Transactional
    public void deleteChickenLeft(Long id, UserPrincipal principal) {
        ChickenLeft chickenLeft = findChickenLeftByIdOrThrow(id);
        validateChickenLeftMutation(chickenLeft, principal, "remover");

        Farm farm = findFarmByIdOrThrow(chickenLeft.getIdFarm());
        int currentChickens = farm.getChickensNow() != null ? farm.getChickensNow() : 0;
        farm.setChickensNow(currentChickens + chickenLeft.getChickensCount());
        farmRepository.save(farm);

        try {
            chickenLeftRepository.delete(chickenLeft);
            chickenLeftRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover o registro de saída de aves pois existem outros dados vinculados a ele",
                    ex
            );
        }
    }

    /**
     * Resolve e valida o ID da fazenda para cadastro de novo registro conforme o perfil autenticado.
     *
     * @param requestedFarmId ID da fazenda enviado no corpo da requisição (pode ser nulo para FARM_OWNER)
     * @param principal       dados do usuário logado
     * @return ID resolvido e validado da fazenda
     */
    private Long resolveAndValidateFarmForCreation(Long requestedFarmId, UserPrincipal principal) {
        String role = principal.getRole();

        if (ADM.equals(role)) {
            if (requestedFarmId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "O ID da fazenda é obrigatório para cadastro por administrador"
                );
            }
            findFarmByIdOrThrow(requestedFarmId);
            return requestedFarmId;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            if (requestedFarmId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "O ID da fazenda é obrigatório para cadastro por funcionário"
                );
            }
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm farm = findFarmByIdOrThrow(requestedFarmId);
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Funcionário não tem permissão para cadastrar registros em fazendas de outra empresa"
                );
            }
            return requestedFarmId;
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Produtor rural não possui fazenda vinculada para registrar saída de aves"
                );
            }
            if (requestedFarmId != null && !Objects.equals(requestedFarmId, owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Produtor rural não tem permissão para cadastrar registros em outra fazenda"
                );
            }
            return owner.getIdFarm();
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para cadastrar registros de saída de aves"
        );
    }

    /**
     * Valida a permissão de leitura sobre um registro de saída de aves.
     *
     * @param chickenLeft registro de saída de aves
     * @param principal   dados do usuário logado
     */
    private void validateChickenLeftAccess(ChickenLeft chickenLeft, UserPrincipal principal) {
        validateChickenLeftMutation(chickenLeft, principal, "acessar");
    }

    /**
     * Valida a permissão de mutação (edição/exclusão) sobre um registro de saída de aves.
     *
     * @param chickenLeft registro de saída de aves
     * @param principal   dados do usuário logado
     * @param action      descrição textual da ação ("alterar" ou "remover")
     */
    private void validateChickenLeftMutation(ChickenLeft chickenLeft, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm farm = findFarmByIdOrThrow(chickenLeft.getIdFarm());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " este registro de saída de aves"
                );
            }
            return;
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(chickenLeft.getIdFarm(), owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " este registro de saída de aves"
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para " + action + " este registro de saída de aves"
        );
    }

    /**
     * Garante que o usuário autenticado esteja presente no contexto de segurança.
     *
     * @param principal dados do usuário logado
     */
    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    /**
     * Busca o funcionário da integradora pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador do funcionário
     * @return funcionário encontrado
     */
    private CompanyEmployee getCompanyEmployeeOrThrow(Long id) {
        return companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionário logado não encontrado para o ID: " + id
                ));
    }

    /**
     * Busca o produtor rural pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador do produtor
     * @return produtor rural encontrado
     */
    private FarmOwner getFarmOwnerOrThrow(Long id) {
        return farmOwnerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produtor rural logado não encontrado para o ID: " + id
                ));
    }

    /**
     * Busca a fazenda pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador da fazenda
     * @return fazenda encontrada
     */
    private Farm findFarmByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Fazenda não encontrada para o ID: null"
            );
        }
        return farmRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + id
                ));
    }

    /**
     * Busca o registro de saída de aves pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador do registro de saída de aves
     * @return registro de saída de aves encontrado
     */
    private ChickenLeft findChickenLeftByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Registro de saída de aves não encontrado para o ID: null"
            );
        }
        return chickenLeftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Registro de saída de aves não encontrado para o ID: " + id
                ));
    }
}
