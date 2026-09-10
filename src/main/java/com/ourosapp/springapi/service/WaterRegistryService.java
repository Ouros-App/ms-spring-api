package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;

import com.ourosapp.springapi.dto.waterregistry.WaterRegistryRequestDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryResponseDTO;
import com.ourosapp.springapi.dto.waterregistry.WaterRegistryUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.WaterRegistry;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.WaterRegistryRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link WaterRegistry}.
 */
@Service
@RequiredArgsConstructor
public class WaterRegistryService {

    private final WaterRegistryRepository waterRegistryRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Cadastra um novo Registro de Medição de Água vinculado a uma Fazenda.
     * Valida as permissões de acesso do usuário autenticado no token JWT.
     *
     * @param request   payload da requisição contendo os dados da medição
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados do registro de medição cadastrado
     * @throws ResponseStatusException HTTP 400 se os dados de medição ou identificação da fazenda forem inválidos
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão para cadastrar na fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda informada não existir
     * @throws ResponseStatusException HTTP 409 se houver conflito de integridade de dados
     */
    @Transactional
    public WaterRegistryResponseDTO createWaterRegistry(WaterRegistryRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Long resolvedFarmId = resolveAndValidateFarmForCreation(request.idFarm(), principal);

        if (request.endHydrometer() != null && request.startHydrometer() != null
                && request.endHydrometer().compareTo(request.startHydrometer()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A leitura final do hidrômetro não pode ser menor que a leitura inicial"
            );
        }

        WaterRegistry waterRegistry = WaterRegistry.builder()
                .registrationDate(request.registrationDate())
                .startHydrometer(request.startHydrometer())
                .endHydrometer(request.endHydrometer())
                .idFarm(resolvedFarmId)
                .build();

        try {
            WaterRegistry saved = waterRegistryRepository.save(waterRegistry);
            return WaterRegistryResponseDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar registro de medição de água",
                    ex
            );
        }
    }

    /**
     * Retorna a lista de registros de medição de água acessíveis ao usuário autenticado.
     * Permite filtrar por um ID de fazenda específico se o usuário tiver permissão.
     *
     * @param farmId    identificador opcional da fazenda para filtro
     * @param principal dados do usuário logado extraídos do token JWT
     * @return lista de DTOs com os registros de medição de água
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão
     * @throws ResponseStatusException HTTP 404 se a fazenda informada não existir
     */
    @Transactional(readOnly = true)
    public List<WaterRegistryResponseDTO> getWaterRegistriesForUser(Long farmId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if ("FARM_OWNER".equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                return List.of();
            }
            if (farmId != null && !Objects.equals(farmId, owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado aos registros de água de outra fazenda"
                );
            }
            return waterRegistryRepository.findAllByIdFarm(owner.getIdFarm())
                    .stream()
                    .map(WaterRegistryResponseDTO::fromEntity)
                    .toList();
        } else if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            if (farmId != null) {
                Farm farm = findFarmByIdOrThrow(farmId);
                if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Acesso negado aos registros de água de fazenda vinculada a outra empresa"
                    );
                }
                return waterRegistryRepository.findAllByIdFarm(farmId)
                        .stream()
                        .map(WaterRegistryResponseDTO::fromEntity)
                        .toList();
            } else {
                List<Farm> enterpriseFarms = farmRepository.findAllByIdEnterprise(employee.getIdEnterprise());
                List<Long> farmIds = enterpriseFarms.stream().map(Farm::getId).toList();
                if (farmIds.isEmpty()) {
                    return List.of();
                }
                return waterRegistryRepository.findAllByIdFarmIn(farmIds)
                        .stream()
                        .map(WaterRegistryResponseDTO::fromEntity)
                        .toList();
            }
        } else if ("ADM".equals(role)) {
            if (farmId != null) {
                findFarmByIdOrThrow(farmId);
                return waterRegistryRepository.findAllByIdFarm(farmId)
                        .stream()
                        .map(WaterRegistryResponseDTO::fromEntity)
                        .toList();
            } else {
                return waterRegistryRepository.findAll()
                        .stream()
                        .map(WaterRegistryResponseDTO::fromEntity)
                        .toList();
            }
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar registros de medição de água"
            );
        }
    }

    /**
     * Busca os detalhes de um registro de medição de água específico pelo seu ID.
     * Valida se o usuário autenticado possui permissão de leitura na fazenda vinculada.
     *
     * @param id        identificador único do registro de medição de água
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os detalhes do registro
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão
     * @throws ResponseStatusException HTTP 404 se o registro não for encontrado
     */
    @Transactional(readOnly = true)
    public WaterRegistryResponseDTO getWaterRegistryById(Long id, UserPrincipal principal) {
        WaterRegistry waterRegistry = findWaterRegistryByIdOrThrow(id);
        validateWaterRegistryAccess(waterRegistry, principal);
        return WaterRegistryResponseDTO.fromEntity(waterRegistry);
    }

    /**
     * Atualiza parcialmente um registro de medição de água existente (PATCH /water-registries/{id}).
     *
     * @param id        identificador único do registro
     * @param request   payload com os campos parciais a serem atualizados
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados atualizados
     * @throws ResponseStatusException HTTP 400 se os novos valores de leitura forem incoerentes
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão
     * @throws ResponseStatusException HTTP 404 se o registro não for encontrado
     */
    @Transactional
    public WaterRegistryResponseDTO updateWaterRegistry(Long id, WaterRegistryUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        WaterRegistry waterRegistry = findWaterRegistryByIdOrThrow(id);
        validateWaterRegistryMutation(waterRegistry, principal, "alterar");

        if (!request.hasUpdates()) {
            return WaterRegistryResponseDTO.fromEntity(waterRegistry);
        }

        BigDecimal effectiveStart = request.startHydrometer() != null
                ? request.startHydrometer()
                : waterRegistry.getStartHydrometer();
        BigDecimal effectiveEnd = request.endHydrometer() != null
                ? request.endHydrometer()
                : waterRegistry.getEndHydrometer();

        if (effectiveEnd != null && effectiveStart != null && effectiveEnd.compareTo(effectiveStart) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A leitura final do hidrômetro não pode ser menor que a leitura inicial"
            );
        }

        if (request.registrationDate() != null) {
            waterRegistry.setRegistrationDate(request.registrationDate());
        }
        if (request.startHydrometer() != null) {
            waterRegistry.setStartHydrometer(request.startHydrometer());
        }
        if (request.endHydrometer() != null) {
            waterRegistry.setEndHydrometer(request.endHydrometer());
        }

        WaterRegistry updated = waterRegistryRepository.save(waterRegistry);
        return WaterRegistryResponseDTO.fromEntity(updated);
    }

    /**
     * Remove um registro de medição de água do sistema.
     *
     * @param id        identificador único do registro a ser removido
     * @param principal dados do usuário logado extraídos do token JWT
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão
     * @throws ResponseStatusException HTTP 404 se o registro não for encontrado
     * @throws ResponseStatusException HTTP 409 se existirem registros dependentes vinculados
     */
    @Transactional
    public void deleteWaterRegistry(Long id, UserPrincipal principal) {
        WaterRegistry waterRegistry = findWaterRegistryByIdOrThrow(id);
        validateWaterRegistryMutation(waterRegistry, principal, "remover");

        try {
            waterRegistryRepository.delete(waterRegistry);
            waterRegistryRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover o registro de água pois existem outros dados vinculados a ele",
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

        if ("ADM".equals(role)) {
            if (requestedFarmId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "O ID da fazenda é obrigatório para cadastro por administrador"
                );
            }
            findFarmByIdOrThrow(requestedFarmId);
            return requestedFarmId;
        }

        if ("COMPANY_EMPLOYEE".equals(role)) {
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

        if ("FARM_OWNER".equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Produtor rural não possui fazenda vinculada para realizar medições"
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
                "Perfil de usuário sem permissão para cadastrar registros de medição de água"
        );
    }

    /**
     * Valida a permissão de leitura sobre um registro de medição de água.
     *
     * @param waterRegistry registro de medição
     * @param principal     dados do usuário logado
     */
    private void validateWaterRegistryAccess(WaterRegistry waterRegistry, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if ("ADM".equals(role)) {
            return;
        }

        if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm farm = findFarmByIdOrThrow(waterRegistry.getIdFarm());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado a este registro de água"
                );
            }
            return;
        }

        if ("FARM_OWNER".equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(waterRegistry.getIdFarm(), owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado a este registro de água"
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para acessar este registro de água"
        );
    }

    /**
     * Valida a permissão de mutação (edição/exclusão) sobre um registro de medição de água.
     *
     * @param waterRegistry registro de medição
     * @param principal     dados do usuário logado
     * @param action        descrição textual da ação ("alterar" ou "remover")
     */
    private void validateWaterRegistryMutation(WaterRegistry waterRegistry, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if ("ADM".equals(role)) {
            return;
        }

        if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm farm = findFarmByIdOrThrow(waterRegistry.getIdFarm());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " este registro de água"
                );
            }
            return;
        }

        if ("FARM_OWNER".equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(waterRegistry.getIdFarm(), owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " este registro de água"
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para " + action + " este registro de água"
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
     * Busca o registro de medição de água pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador do registro de água
     * @return registro de medição encontrado
     */
    private WaterRegistry findWaterRegistryByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Registro de água não encontrado para o ID: null"
                );
        }
        return waterRegistryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Registro de água não encontrado para o ID: " + id
                ));
    }
}
