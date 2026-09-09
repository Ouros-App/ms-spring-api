package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.EMPLOYEE_NOT_FOUND;
import static com.ourosapp.springapi.constants.ErrorMessages.FARM_OWNER_NOT_FOUND;
import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.farmowner.FarmOwnerRequestDTO;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerResponseDTO;
import com.ourosapp.springapi.dto.farmowner.FarmOwnerUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link FarmOwner}.
 */
@Service
@RequiredArgsConstructor
public class FarmOwnerService {

    private final FarmOwnerRepository farmOwnerRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cadastra um novo Produtor Rural vinculado a uma Fazenda.
     * Valida permissões (apenas ADM ou Funcionário da mesma empresa integradora à qual a fazenda pertence).
     *
     * @param request   payload com os dados cadastrais do produtor rural
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO contendo os dados do produtor cadastrado (sem a senha)
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para cadastrar nesta fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda vinculada não for encontrada
     * @throws ResponseStatusException HTTP 409 se CPF ou e-mail já estiverem cadastrados
     */
    @Transactional
    public FarmOwnerResponseDTO createFarmOwner(FarmOwnerRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Farm farm = farmRepository.findById(request.idFarm())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + request.idFarm()
                ));

        validateFarmOwnerCreationPermission(farm, principal);

        if (farmOwnerRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um produtor rural cadastrado com este documento"
            );
        }

        if (farmOwnerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um produtor rural cadastrado com este e-mail"
            );
        }

        String encryptedPassword = passwordEncoder.encode(request.password());

        FarmOwner farmOwner = FarmOwner.builder()
                .name(request.name())
                .documentNumber(request.documentNumber())
                .email(request.email())
                .telephone(request.telephone())
                .password(encryptedPassword)
                .idFarm(request.idFarm())
                .build();

        try {
            FarmOwner savedFarmOwner = farmOwnerRepository.save(farmOwner);
            return FarmOwnerResponseDTO.fromEntity(savedFarmOwner);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar produtor rural",
                    ex
            );
        }
    }

    /**
     * Retorna as informações do produtor rural atualmente autenticado via token JWT.
     *
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados do produtor rural autenticado
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil logado não for de produtor rural
     * @throws ResponseStatusException HTTP 404 se o produtor não for encontrado no banco de dados
     */
    @Transactional(readOnly = true)
    public FarmOwnerResponseDTO getLoggedInFarmOwner(UserPrincipal principal) {
        ensureAuthenticated(principal);

        if (!FARM_OWNER.equals(principal.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso restrito a produtores rurais"
            );
        }

        FarmOwner owner = farmOwnerRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produtor rural logado não encontrado para o ID: " + principal.getId()
                ));

        return FarmOwnerResponseDTO.fromEntity(owner);
    }

    /**
     * Lista os produtores rurais acessíveis pelo usuário autenticado, com suporte a filtro opcional por fazenda.
     * <ul>
     *     <li>{@code ADM}: Lista todos os produtores ou filtra pela fazenda informada se especificada.</li>
     *     <li>{@code COMPANY_EMPLOYEE}: Lista produtores de todas as fazendas da sua empresa integradora, ou de uma fazenda específica se pertencer à empresa.</li>
     *     <li>{@code FARM_OWNER}: Lista o próprio produtor ou produtores da mesma fazenda.</li>
     * </ul>
     *
     * @param farmId    filtro opcional pelo identificador único da fazenda
     * @param principal dados do usuário logado extraídos do token JWT
     * @return lista de DTOs dos produtores rurais encontrados
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para listar os produtores da fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda informada ou dados do usuário não forem encontrados
     */
    @Transactional(readOnly = true)
    public List<FarmOwnerResponseDTO> getFarmOwners(Long farmId, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();

        if (ADM.equals(role)) {
            if (farmId != null) {
                if (!farmRepository.existsById(farmId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: " + farmId);
                }
                return farmOwnerRepository.findAllByIdFarm(farmId)
                        .stream()
                        .map(FarmOwnerResponseDTO::fromEntity)
                        .toList();
            }
            return farmOwnerRepository.findAll()
                    .stream()
                    .map(FarmOwnerResponseDTO::fromEntity)
                    .toList();
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());

            if (farmId != null) {
                Farm farm = farmRepository.findById(farmId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: " + farmId));

                if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Funcionário não tem permissão para visualizar produtores rurais de fazenda de outra integradora"
                    );
                }
                return farmOwnerRepository.findAllByIdFarm(farmId)
                        .stream()
                        .map(FarmOwnerResponseDTO::fromEntity)
                        .toList();
            }

            List<Farm> employeeFarms = farmRepository.findAllByIdEnterprise(employee.getIdEnterprise());
            List<Long> employeeFarmIds = employeeFarms.stream().map(Farm::getId).toList();

            if (employeeFarmIds.isEmpty()) {
                return List.of();
            }

            return farmOwnerRepository.findAllByIdFarmIn(employeeFarmIds)
                    .stream()
                    .map(FarmOwnerResponseDTO::fromEntity)
                    .toList();
        }

        if (FARM_OWNER.equals(role)) {
            FarmOwner currentOwner = farmOwnerRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Produtor rural logado não encontrado para o ID: " + principal.getId()
                    ));

            if (farmId != null && !Objects.equals(farmId, currentOwner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Produtor rural só pode consultar produtores da sua própria fazenda"
                );
            }

            if (currentOwner.getIdFarm() == null) {
                return List.of(FarmOwnerResponseDTO.fromEntity(currentOwner));
            }

            return farmOwnerRepository.findAllByIdFarm(currentOwner.getIdFarm())
                    .stream()
                    .map(FarmOwnerResponseDTO::fromEntity)
                    .toList();
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para listar produtores rurais"
        );
    }

    /**
     * Busca as informações de um produtor rural específico através do seu ID.
     * Valida permissões de visualização (ADM, funcionário da mesma integradora ou o próprio produtor).
     *
     * @param id        identificador único do produtor rural
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados do produtor rural encontrado
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para visualizar
     * @throws ResponseStatusException HTTP 404 se o produtor rural não for encontrado
     */
    @Transactional(readOnly = true)
    public FarmOwnerResponseDTO getFarmOwnerById(Long id, UserPrincipal principal) {
        FarmOwner owner = findFarmOwnerByIdOrThrow(id);
        validateFarmOwnerAccessPermission(owner, principal);
        return FarmOwnerResponseDTO.fromEntity(owner);
    }

    /**
     * Atualiza parcialmente os dados cadastrais de um produtor rural (telefone, e-mail e/ou senha).
     * Valida se o usuário tem permissão para alterar os dados (ADM, funcionário da mesma integradora ou o próprio produtor).
     *
     * @param id        identificador único do produtor rural a ser atualizado
     * @param request   novos dados parciais a serem aplicados
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados atualizados do produtor rural
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para atualizar
     * @throws ResponseStatusException HTTP 404 se o produtor rural não for encontrado
     * @throws ResponseStatusException HTTP 409 se o novo e-mail já pertencer a outro produtor rural
     */
    @Transactional
    public FarmOwnerResponseDTO updateFarmOwner(Long id, FarmOwnerUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        FarmOwner owner = findFarmOwnerByIdOrThrow(id);
        validateFarmOwnerAccessPermission(owner, principal);

        if (!request.hasUpdates()) {
            return FarmOwnerResponseDTO.fromEntity(owner);
        }

        if (request.email() != null && !request.email().isBlank()) {
            farmOwnerRepository.findByEmailIgnoreCase(request.email())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Já existe outro produtor rural cadastrado com este e-mail"
                        );
                    });
            owner.setEmail(request.email());
        }

        if (request.telephone() != null && !request.telephone().isBlank()) {
            owner.setTelephone(request.telephone());
        }

        if (request.password() != null && !request.password().isBlank()) {
            owner.setPassword(passwordEncoder.encode(request.password()));
        }

        try {
            FarmOwner updatedOwner = farmOwnerRepository.save(owner);
            return FarmOwnerResponseDTO.fromEntity(updatedOwner);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao atualizar produtor rural",
                    ex
            );
        }
    }

    /**
     * Remove um produtor rural do sistema.
     * Valida se o usuário autenticado é ADM ou funcionário da empresa integradora vinculada à fazenda.
     *
     * @param id        identificador único do produtor rural a ser removido
     * @param principal dados do usuário logado extraídos do token JWT
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para remover
     * @throws ResponseStatusException HTTP 404 se o produtor rural não for encontrado
     * @throws ResponseStatusException HTTP 409 se houver registros vinculados impedindo a exclusão
     */
    @Transactional
    public void deleteFarmOwner(Long id, UserPrincipal principal) {
        FarmOwner owner = findFarmOwnerByIdOrThrow(id);
        validateFarmOwnerDeletionPermission(owner, principal);

        try {
            farmOwnerRepository.delete(owner);
            farmOwnerRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover o produtor rural pois existem registros vinculados a ele",
                    ex
            );
        }
    }

    /**
     * Valida se o usuário autenticado tem permissão para cadastrar um produtor rural na fazenda informada.
     * Administradores globais têm permissão irrestrita. Funcionários só podem cadastrar em fazendas da sua integradora.
     *
     * @param farm      entidade da fazenda à qual o produtor será vinculado
     * @param principal dados do usuário autenticado no token JWT
     * @throws ResponseStatusException HTTP 401 se o principal for nulo ou sem ID
     * @throws ResponseStatusException HTTP 403 se o perfil for não autorizado ou pertencer a outra empresa integradora
     * @throws ResponseStatusException HTTP 404 se o registro do funcionário logado não for encontrado
     */
    private void validateFarmOwnerCreationPermission(Farm farm, UserPrincipal principal) {
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
                        "Funcionário não tem permissão para cadastrar produtor rural em fazenda de outra empresa integradora"
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para cadastrar produtores rurais"
        );
    }

    /**
     * Valida se o usuário autenticado tem permissão para acessar ou atualizar os dados do produtor rural especificado.
     * Administradores têm acesso total. Produtores rurais só podem acessar os seus próprios dados.
     * Funcionários corporativos só podem acessar dados de produtores vinculados a fazendas da sua empresa integradora.
     *
     * @param owner     entidade do produtor rural alvo
     * @param principal dados do usuário autenticado no token JWT
     * @throws ResponseStatusException HTTP 401 se o principal for nulo ou sem ID
     * @throws ResponseStatusException HTTP 403 se o usuário não possuir privilégios suficientes
     * @throws ResponseStatusException HTTP 404 se o registro do funcionário logado não for encontrado
     */
    private void validateFarmOwnerAccessPermission(FarmOwner owner, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }

        if (FARM_OWNER.equals(role)) {
            if (Objects.equals(owner.getId(), principal.getId())) {
                return;
            }
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso negado aos dados de outro produtor rural"
            );
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm farm = owner.getIdFarm() != null ? farmRepository.findById(owner.getIdFarm()).orElse(null) : null;
            if (farm != null && Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                return;
            }
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso negado a este produtor rural"
            );
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para acessar este produtor rural"
        );
    }

    /**
     * Valida se o usuário autenticado tem permissão para remover o cadastro do produtor rural.
     * Administradores e funcionários da empresa integradora responsável pela fazenda podem remover.
     * Produtores rurais não possuem permissão de autoexclusão direta.
     *
     * @param owner     entidade do produtor rural a ser removido
     * @param principal dados do usuário autenticado no token JWT
     * @throws ResponseStatusException HTTP 401 se o principal for nulo ou sem ID
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão para remoção
     * @throws ResponseStatusException HTTP 404 se o registro do funcionário logado não for encontrado
     */
    private void validateFarmOwnerDeletionPermission(FarmOwner owner, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }

        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            Farm farm = owner.getIdFarm() != null ? farmRepository.findById(owner.getIdFarm()).orElse(null) : null;
            if (farm != null && Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                return;
            }
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acesso negado para remover este produtor rural"
            );
        }

        if (FARM_OWNER.equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Produtor rural não possui permissão para remover cadastros de produtores rurais"
            );
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para remover produtores rurais"
        );
    }

    /**
     * Garante que o usuário autenticado esteja presente no contexto de segurança.
     *
     * @param principal dados do usuário autenticado
     * @throws ResponseStatusException HTTP 401 se o principal for nulo ou não possuir identificador
     */
    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    /**
     * Recupera a entidade {@link CompanyEmployee} associada ao ID informado ou lança HTTP 404.
     *
     * @param id identificador único do funcionário
     * @return entidade do funcionário corporativo encontrada
     * @throws ResponseStatusException HTTP 404 se o funcionário não for encontrado
     */
    private CompanyEmployee getCompanyEmployeeOrThrow(Long id) {
        return companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        EMPLOYEE_NOT_FOUND
                ));
    }

    /**
     * Recupera a entidade {@link FarmOwner} pelo ID ou lança HTTP 404 se não for encontrada.
     *
     * @param id identificador único do produtor rural
     * @return entidade do produtor rural encontrada
     * @throws ResponseStatusException HTTP 404 se o ID for nulo ou o produtor não existir
     */
    private FarmOwner findFarmOwnerByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    FARM_OWNER_NOT_FOUND + " para o ID: null"
            );
        }
        return farmOwnerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        FARM_OWNER_NOT_FOUND + " para o ID: " + id
                ));
    }
}
