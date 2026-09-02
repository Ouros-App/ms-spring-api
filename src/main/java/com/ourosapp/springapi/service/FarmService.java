package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.AddressResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.repository.AddressRepository;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
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
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link Farm}.
 */
@Service
@RequiredArgsConstructor
public class FarmService {

    private final FarmRepository farmRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final AddressRepository addressRepository;
    private final AddressService addressService;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Cadastra uma nova Fazenda vinculada à Empresa Integradora e ao Endereço.
     * Valida as permissões do usuário logado (apenas ADM ou Funcionário da mesma empresa).
     * Suporta endereço pré-existente via {@code id_address} ou criação composta de novo endereço via {@code address}.
     *
     * @param request   payload da requisição contendo os dados da fazenda e endereço
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados da fazenda cadastrada
     * @throws ResponseStatusException HTTP 400 se nenhuma informação de endereço for fornecida
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para cadastrar na empresa informada
     * @throws ResponseStatusException HTTP 404 se a empresa ou endereço vinculado não existirem
     * @throws ResponseStatusException HTTP 409 se houver conflito de integridade de dados ao cadastrar fazenda
     */
    @Transactional
    public FarmResponseDTO createFarm(FarmRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        if (request.idEnterprise() == null || !enterpriseRepository.existsById(request.idEnterprise())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Empresa integradora não encontrada para o ID: " + request.idEnterprise()
            );
        }

        validateFarmCreationPermission(request.idEnterprise(), principal);

        Long resolvedAddressId;
        if (request.address() != null && request.idAddress() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não é permitido informar 'id_address' e o objeto 'address' simultaneamente"
            );
        } else if (request.address() != null) {
            AddressResponseDTO createdAddress = addressService.createAddress(request.address());
            resolvedAddressId = createdAddress.id();
        } else if (request.idAddress() != null) {
            if (!addressRepository.existsById(request.idAddress())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o ID: " + request.idAddress()
                );
            }
            resolvedAddressId = request.idAddress();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "É obrigatório informar o 'id_address' ou o objeto 'address' completo"
            );
        }

        Farm farm = Farm.builder()
                .name(request.name())
                .areaProperty(request.areaProperty())
                .region(request.region())
                .poultryCapacity(request.poultryCapacity())
                .place(request.place())
                .idAddress(resolvedAddressId)
                .idEnterprise(request.idEnterprise())
                .build();

        try {
            Farm savedFarm = farmRepository.save(farm);
            return FarmResponseDTO.fromEntity(savedFarm);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar fazenda",
                    ex
            );
        }
    }

    /**
     * Retorna a lista de fazendas vinculadas ao usuário autenticado no JWT.
     * <ul>
     *     <li>{@code COMPANY_EMPLOYEE}: Retorna todas as fazendas da empresa integradora à qual pertence.</li>
     *     <li>{@code FARM_OWNER}: Retorna a fazenda vinculada ao seu cadastro de produtor.</li>
     *     <li>{@code ADM}: Retorna todas as fazendas cadastradas no sistema.</li>
     * </ul>
     *
     * @param principal dados do usuário logado extraídos do token JWT
     * @return lista de DTOs das fazendas associadas
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão
     * @throws ResponseStatusException HTTP 404 se os dados do usuário não forem encontrados no banco
     */
    @Transactional(readOnly = true)
    public List<FarmResponseDTO> getFarmsForUser(UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            return farmRepository.findAllByIdEnterprise(employee.getIdEnterprise())
                    .stream()
                    .map(FarmResponseDTO::fromEntity)
                    .toList();
        } else if ("FARM_OWNER".equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (owner.getIdFarm() == null) {
                return List.of();
            }
            return farmRepository.findById(owner.getIdFarm())
                    .map(farm -> List.of(FarmResponseDTO.fromEntity(farm)))
                    .orElseGet(List::of);
        } else if ("ADM".equals(role)) {
            return farmRepository.findAll()
                    .stream()
                    .map(FarmResponseDTO::fromEntity)
                    .toList();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar fazendas"
            );
        }
    }

    /**
     * Busca os detalhes de uma fazenda específica pelo seu identificador único.
     * Valida se o usuário autenticado possui vínculo e autorização para visualizar a fazenda.
     *
     * @param id        identificador único da fazenda
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com as informações da fazenda
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para acessar a fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda não for encontrada
     */
    @Transactional(readOnly = true)
    public FarmResponseDTO getFarmById(Long id, UserPrincipal principal) {
        Farm farm = findFarmByIdOrThrow(id);
        validateFarmAccessPermission(farm, principal);
        return FarmResponseDTO.fromEntity(farm);
    }

    /**
     * Atualiza dados pontuais de uma fazenda existente (PATCH /farms/{id}).
     * Valida se o usuário autenticado possui vínculo e autorização para editar a fazenda.
     *
     * @param id        identificador único da fazenda a ser atualizada
     * @param request   novos dados parciais da fazenda
     * @param principal dados do usuário logado extraídos do token JWT
     * @return DTO com os dados atualizados da fazenda
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para atualizar a fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda não for encontrada
     */
    @Transactional
    public FarmResponseDTO updateFarm(Long id, FarmUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        Farm farm = findFarmByIdOrThrow(id);
        validateFarmMutationPermission(farm, principal, "alterar");

        if (!request.hasUpdates()) {
            return FarmResponseDTO.fromEntity(farm);
        }

        if (request.name() != null && !request.name().isBlank()) {
            farm.setName(request.name());
        }
        if (request.areaProperty() != null) {
            farm.setAreaProperty(request.areaProperty());
        }
        if (request.region() != null && !request.region().isBlank()) {
            farm.setRegion(request.region());
        }
        if (request.poultryCapacity() != null) {
            farm.setPoultryCapacity(request.poultryCapacity());
        }
        if (request.place() != null && !request.place().isBlank()) {
            farm.setPlace(request.place());
        }

        Farm updatedFarm = farmRepository.save(farm);
        return FarmResponseDTO.fromEntity(updatedFarm);
    }

    /**
     * Remove uma fazenda do sistema.
     * Valida se o usuário autenticado possui vínculo de administrador ou funcionário da empresa vinculada.
     *
     * @param id        identificador único da fazenda a ser removida
     * @param principal dados do usuário logado extraídos do token JWT
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não tiver permissão para remover a fazenda
     * @throws ResponseStatusException HTTP 404 se a fazenda não for encontrada
     */
    @Transactional
    public void deleteFarm(Long id, UserPrincipal principal) {
        Farm farm = findFarmByIdOrThrow(id);
        validateFarmMutationPermission(farm, principal, "remover");
        try {
            farmRepository.delete(farm);
            farmRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover a fazenda pois existem registros vinculados a ela",
                    ex
            );
        }
    }

    /**
     * Valida se o usuário autenticado tem permissão para cadastrar uma nova fazenda vinculada à empresa especificada.
     *
     * @param idEnterprise ID da empresa integradora onde a fazenda será cadastrada
     * @param principal    dados do usuário logado
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão ou pertencer a outra empresa
     * @throws ResponseStatusException HTTP 404 se o funcionário não for encontrado
     */
    private void validateFarmCreationPermission(Long idEnterprise, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if ("ADM".equals(role)) {
            return;
        }

        if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            if (!Objects.equals(idEnterprise, employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Funcionário não tem permissão para cadastrar fazendas em outra empresa integradora"
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para cadastrar fazendas"
        );
    }

    /**
     * Valida se o usuário autenticado possui permissão de leitura na fazenda especificada.
     *
     * @param farm      entidade da fazenda a ser acessada
     * @param principal dados do usuário logado
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão ou pertencer a outra empresa/fazenda
     * @throws ResponseStatusException HTTP 404 se o usuário vinculado não for encontrado
     */
    private void validateFarmAccessPermission(Farm farm, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if ("ADM".equals(role)) {
            return;
        }

        if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado a esta fazenda"
                );
            }
            return;
        }

        if ("FARM_OWNER".equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(farm.getId(), owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado a esta fazenda"
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
     * Valida se o usuário autenticado possui permissão para operações de mutação (alteração ou exclusão) na fazenda especificada.
     * Somente ADM e COMPANY_EMPLOYEE da mesma empresa podem alterar ou remover fazendas.
     *
     * @param farm      entidade da fazenda a ser manipulada
     * @param principal dados do usuário logado
     * @param action    descrição textual da ação ("alterar" ou "remover")
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o perfil não tiver permissão ou pertencer a outra empresa
     * @throws ResponseStatusException HTTP 404 se o funcionário vinculado não for encontrado
     */
    private void validateFarmMutationPermission(Farm farm, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if ("ADM".equals(role)) {
            return;
        }

        if ("COMPANY_EMPLOYEE".equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " esta fazenda"
                );
            }
            return;
        }

        if ("FARM_OWNER".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Produtor rural não possui permissão para " + action + " fazendas"
            );
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para " + action + " fazendas"
        );
    }

    /**
     * Garante que os dados do usuário autenticado estejam presentes.
     *
     * @param principal dados do usuário logado
     * @throws ResponseStatusException HTTP 401 se principal ou seu ID forem nulos
     */
    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
    }

    /**
     * Busca o funcionário da integradora pelo ID ou lança HTTP 404 Not Found.
     *
     * @param id identificador do funcionário
     * @return entidade do funcionário encontrada
     * @throws ResponseStatusException HTTP 404 se não encontrado
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
     * @param id identificador do produtor rural
     * @return entidade do produtor rural encontrada
     * @throws ResponseStatusException HTTP 404 se não encontrado
     */
    private FarmOwner getFarmOwnerOrThrow(Long id) {
        return farmOwnerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produtor rural logado não encontrado para o ID: " + id
                ));
    }

    /**
     * Busca a fazenda pelo ID ou lança HTTP 404 Not Found se não for encontrada ou se o ID for nulo.
     *
     * @param id identificador da fazenda
     * @return entidade da fazenda encontrada
     * @throws ResponseStatusException HTTP 404 se a fazenda não for encontrada ou se o ID for nulo
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
}
