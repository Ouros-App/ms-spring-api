package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryRequestDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryResponseDTO;
import com.ourosapp.springapi.dto.energyregistry.EnergyRegistryUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.EnergyRegistry;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnergyRegistryRepository;
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
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link EnergyRegistry}.
 */
@Service
@RequiredArgsConstructor
public class EnergyRegistryService {

    private final EnergyRegistryRepository energyRegistryRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Cadastra um novo registro de consumo de energia elétrica para a fazenda.
     * Valida as permissões do usuário logado conforme vínculo com a fazenda ou empresa integradora.
     *
     * @param request   payload com a data, consumo e ID da fazenda
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados do registro cadastrado
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se não autorizado para a fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda não for encontrada
     * @throws ResponseStatusException HTTP 409 se houver conflito de integridade
     */
    @Transactional
    public EnergyRegistryResponseDTO createEnergyRegistry(EnergyRegistryRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Long farmId = resolveFarmIdForCreation(request.idFarm(), principal);
        Farm farm = findFarmByIdOrThrow(farmId);
        validateFarmAccessPermission(farm, principal, "cadastrar registros de energia nesta fazenda");

        EnergyRegistry registry = EnergyRegistry.builder()
                .registrationDate(request.registrationDate())
                .energyConsumption(request.energyConsumption())
                .idFarm(farm.getId())
                .build();

        try {
            EnergyRegistry saved = energyRegistryRepository.save(registry);
            return EnergyRegistryResponseDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar registro de energia",
                    ex
            );
        }
    }

    /**
     * Retorna a lista de registros de consumo de energia acessíveis ao usuário autenticado.
     * Opcionalmente filtra por uma fazenda específica via {@code farmIdFilter}.
     *
     * @param farmIdFilter ID opcional da fazenda para filtragem
     * @param principal    dados do usuário logado extraídos do token JWT
     * @return lista de DTOs dos registros de energia encontrados
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se tentar filtrar por uma fazenda sem acesso
     * @throws ResponseStatusException HTTP 404 se a fazenda filtrada não for encontrada
     */
    @Transactional(readOnly = true)
    public List<EnergyRegistryResponseDTO> getEnergyRegistriesForUser(Long farmIdFilter, UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (farmIdFilter != null) {
            Farm farm = findFarmByIdOrThrow(farmIdFilter);
            validateFarmAccessPermission(farm, principal, "visualizar registros de energia desta fazenda");
            return energyRegistryRepository.findByIdFarm(farm.getId())
                    .stream()
                    .map(EnergyRegistryResponseDTO::fromEntity)
                    .toList();
        }

        String role = principal.getRole();
        if (ADM.equals(role)) {
            return energyRegistryRepository.findAll()
                    .stream()
                    .map(EnergyRegistryResponseDTO::fromEntity)
                    .toList();
        } else if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            List<Long> farmIds = farmRepository.findAllByIdEnterprise(employee.getIdEnterprise())
                    .stream()
                    .map(Farm::getId)
                    .toList();
            if (farmIds.isEmpty()) {
                return List.of();
            }
            return energyRegistryRepository.findByIdFarmIn(farmIds)
                    .stream()
                    .map(EnergyRegistryResponseDTO::fromEntity)
                    .toList();
        } else if (FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                return List.of();
            }
            return energyRegistryRepository.findByIdFarm(owner.getIdFarm())
                    .stream()
                    .map(EnergyRegistryResponseDTO::fromEntity)
                    .toList();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar registros de energia"
            );
        }
    }

    /**
     * Busca os detalhes de 1 registro de consumo de energia pelo seu ID.
     *
     * @param id        identificador único do registro
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os detalhes do registro
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se não tiver permissão para a fazenda vinculada
     * @throws ResponseStatusException HTTP 404 se o registro de energia ou a fazenda não forem encontrados
     */
    @Transactional(readOnly = true)
    public EnergyRegistryResponseDTO getEnergyRegistryById(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        EnergyRegistry registry = findEnergyRegistryByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(registry.getIdFarm());
        validateFarmAccessPermission(farm, principal, "visualizar registros desta fazenda");

        return EnergyRegistryResponseDTO.fromEntity(registry);
    }

    /**
     * Atualiza parcialmente dados de um registro de consumo de energia existente (PATCH /energy-registries/{id}).
     *
     * @param id        identificador único do registro a ser atualizado
     * @param request   payload contendo os campos opcionais de atualização (data e/ou consumo)
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados atualizados do registro
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se não tiver permissão para a fazenda vinculada
     * @throws ResponseStatusException HTTP 404 se o registro ou a fazenda não forem encontrados
     * @throws ResponseStatusException HTTP 409 se houver conflito de integridade
     */
    @Transactional
    public EnergyRegistryResponseDTO updateEnergyRegistry(Long id, EnergyRegistryUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        EnergyRegistry registry = findEnergyRegistryByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(registry.getIdFarm());
        validateFarmAccessPermission(farm, principal, "alterar registros de energia desta fazenda");

        if (!request.hasUpdates()) {
            return EnergyRegistryResponseDTO.fromEntity(registry);
        }

        if (request.registrationDate() != null) {
            registry.setRegistrationDate(request.registrationDate());
        }
        if (request.energyConsumption() != null) {
            registry.setEnergyConsumption(request.energyConsumption());
        }

        try {
            EnergyRegistry updated = energyRegistryRepository.save(registry);
            return EnergyRegistryResponseDTO.fromEntity(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao atualizar registro de energia",
                    ex
            );
        }
    }

    /**
     * Remove um registro de consumo de energia pelo seu ID.
     *
     * @param id        identificador único do registro a ser removido
     * @param principal dados do usuário logado extraídos do token JWT
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se não tiver permissão para a fazenda vinculada
     * @throws ResponseStatusException HTTP 404 se o registro não for encontrado
     */
    @Transactional
    public void deleteEnergyRegistry(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);

        EnergyRegistry registry = findEnergyRegistryByIdOrThrow(id);
        Farm farm = findFarmByIdOrThrow(registry.getIdFarm());
        validateFarmAccessPermission(farm, principal, "remover registro de energia desta fazenda");

        energyRegistryRepository.delete(registry);
    }

    /**
     * Valida se o usuário autenticado tem permissão para acessar/manipular registros da fazenda especificada.
     *
     * @param farm      entidade da fazenda
     * @param principal dados do usuário logado
     * @param action    descrição textual da ação para compor a mensagem de erro
     */
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

    /**
     * Resolve o identificador da fazenda para cadastro de registro de energia.
     * Caso o produtor rural não envie o ID da fazenda no payload, utiliza a fazenda vinculada ao seu cadastro.
     * Para administradores e funcionários da empresa integradora, o envio explícito do ID da fazenda é obrigatório.
     *
     * @param idFarm    ID da fazenda informado no DTO (pode ser nulo)
     * @param principal dados do usuário logado
     * @return ID da fazenda resolvido
     * @throws ResponseStatusException HTTP 400 se o ID for nulo para ADM/Funcionário ou se o produtor não tiver fazenda vinculada
     */
    private Long resolveFarmIdForCreation(Long idFarm, UserPrincipal principal) {
        if (idFarm != null) {
            return idFarm;
        }

        if (FARM_OWNER.equals(principal.getRole())) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Produtor rural logado não possui fazenda vinculada para registrar consumo"
                );
            }
            return owner.getIdFarm();
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "O ID da fazenda é obrigatório para administradores e funcionários da empresa"
        );
    }

    /**
     * Garante que os dados do usuário autenticado estejam presentes.
     */
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

    private EnergyRegistry findEnergyRegistryByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de energia não encontrado para o ID: null");
        }
        return energyRegistryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Registro de energia não encontrado para o ID: " + id
                ));
    }
}
