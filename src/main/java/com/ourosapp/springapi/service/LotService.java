package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;

import com.ourosapp.springapi.constants.RoleConstants;
import com.ourosapp.springapi.dto.lot.LotRequestDTO;
import com.ourosapp.springapi.dto.lot.LotResponseDTO;
import com.ourosapp.springapi.dto.lot.LotUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.Lot;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.EnterpriseRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.LotRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pela lógica de negócios e operações de persistência da entidade {@link Lot}.
 */
@Service
@RequiredArgsConstructor
public class LotService {

    private final LotRepository lotRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Inicia e cadastra um novo Lote de Aves vinculado a uma Fazenda e a uma Empresa Integradora.
     * Valida permissões RBAC e integridade relacional entre Fazenda e Empresa.
     *
     * @param request   payload com as informações do lote
     * @param principal usuário autenticado no JWT
     * @return DTO contendo os dados do lote cadastrado
     * @throws ResponseStatusException HTTP 400 se a fazenda não pertencer à empresa informada
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não possuir permissão para criar lotes na empresa
     * @throws ResponseStatusException HTTP 404 se a empresa ou fazenda não existirem
     * @throws ResponseStatusException HTTP 409 se houver conflito de integridade de dados
     */
    @Transactional
    public LotResponseDTO createLot(LotRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        ensureAuthenticated(principal);

        Farm farm = farmRepository.findById(request.idFarm())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + request.idFarm()
                ));

        Long resolvedEnterpriseId = resolveEnterpriseIdForCreation(request, farm, principal);
        validateEnterpriseForFarm(farm, resolvedEnterpriseId);

        Lot lot = buildLotFromRequest(request, resolvedEnterpriseId);

        try {
            Lot savedLot = lotRepository.save(lot);
            return LotResponseDTO.fromEntity(savedLot);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados ao cadastrar lote",
                    ex
            );
        }
    }

    /**
     * Retorna a lista de lotes disponíveis de acordo com o perfil do usuário logado e filtros opcionais.
     *
     * @param idFarm       filtro opcional por fazenda
     * @param idEnterprise filtro opcional por empresa integradora
     * @param principal    usuário autenticado no JWT
     * @return lista de DTOs de lotes
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário tentar consultar lotes fora do seu escopo
     * @throws ResponseStatusException HTTP 404 se o registro do usuário autenticado ou a fazenda filtrada não existirem
     */
    @Transactional(readOnly = true)
    public List<LotResponseDTO> getLotsForUser(Long idFarm, Long idEnterprise, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (RoleConstants.ADM.equals(role)) {
            return getLotsForAdm(idFarm, idEnterprise);
        }
        if (RoleConstants.COMPANY_EMPLOYEE.equals(role)) {
            return getLotsForCompanyEmployee(idFarm, idEnterprise, principal.getId());
        }
        if (RoleConstants.FARM_OWNER.equals(role)) {
            return getLotsForFarmOwner(idFarm, idEnterprise, principal.getId());
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para listar lotes"
        );
    }

    private Long resolveEnterpriseIdForCreation(LotRequestDTO request, Farm farm, UserPrincipal principal) {
        String role = principal.getRole();
        Long requestedEnterpriseId = request.idEnterprise();

        if (RoleConstants.COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            if (requestedEnterpriseId != null && !Objects.equals(requestedEnterpriseId, employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Funcionário não tem permissão para cadastrar lotes em outra empresa integradora"
                );
            }
            return employee.getIdEnterprise();
        }

        if (RoleConstants.ADM.equals(role)) {
            return requestedEnterpriseId != null ? requestedEnterpriseId : farm.getIdEnterprise();
        }

        if (RoleConstants.FARM_OWNER.equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Produtor rural não possui permissão para cadastrar lotes"
            );
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para cadastrar lotes"
        );
    }

    private void validateEnterpriseForFarm(Farm farm, Long resolvedEnterpriseId) {
        if (resolvedEnterpriseId == null || !enterpriseRepository.existsById(resolvedEnterpriseId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Empresa integradora não encontrada para o ID: " + resolvedEnterpriseId
            );
        }

        if (!Objects.equals(farm.getIdEnterprise(), resolvedEnterpriseId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A fazenda informada não pertence à empresa integradora especificada"
            );
        }
    }

    private Lot buildLotFromRequest(LotRequestDTO request, Long resolvedEnterpriseId) {
        Integer deliveredChickens = request.deliveredChickens() != null ? request.deliveredChickens() : 0;
        Integer losts = request.losts() != null ? request.losts() : 0;
        Double cost = request.cost() != null ? request.cost() : 0.0;

        return Lot.builder()
                .receivedChickens(request.receivedChickens())
                .deliveredChickens(deliveredChickens)
                .deliveryDate(request.deliveryDate())
                .losts(losts)
                .cost(cost)
                .idEnterprise(resolvedEnterpriseId)
                .idFarm(request.idFarm())
                .build();
    }

    private List<LotResponseDTO> getLotsForAdm(Long idFarm, Long idEnterprise) {
        List<Lot> lots;
        if (idEnterprise != null && idFarm != null) {
            lots = lotRepository.findAllByIdEnterpriseAndIdFarm(idEnterprise, idFarm);
        } else if (idEnterprise != null) {
            lots = lotRepository.findAllByIdEnterprise(idEnterprise);
        } else if (idFarm != null) {
            lots = lotRepository.findAllByIdFarm(idFarm);
        } else {
            lots = lotRepository.findAll();
        }
        return lots.stream().map(LotResponseDTO::fromEntity).toList();
    }

    private List<LotResponseDTO> getLotsForCompanyEmployee(Long idFarm, Long idEnterprise, Long employeeId) {
        CompanyEmployee employee = getCompanyEmployeeOrThrow(employeeId);
        if (idEnterprise != null && !Objects.equals(idEnterprise, employee.getIdEnterprise())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Funcionário não tem permissão para visualizar lotes de outra empresa integradora"
            );
        }

        if (idFarm != null) {
            Farm farm = farmRepository.findById(idFarm)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Fazenda não encontrada para o ID: " + idFarm
                    ));
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado a lotes de fazenda pertencente a outra integradora"
                );
            }
            return lotRepository.findAllByIdEnterpriseAndIdFarm(employee.getIdEnterprise(), idFarm)
                    .stream()
                    .map(LotResponseDTO::fromEntity)
                    .toList();
        }

        return lotRepository.findAllByIdEnterprise(employee.getIdEnterprise())
                .stream()
                .map(LotResponseDTO::fromEntity)
                .toList();
    }

    private List<LotResponseDTO> getLotsForFarmOwner(Long idFarm, Long idEnterprise, Long ownerId) {
        FarmOwner owner = getFarmOwnerOrThrow(ownerId);
        if (owner.getIdFarm() == null) {
            return List.of();
        }

        if (idFarm != null && !Objects.equals(idFarm, owner.getIdFarm())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Produtor rural não tem permissão para visualizar lotes de outra fazenda"
            );
        }

        if (idEnterprise != null) {
            Farm farm = farmRepository.findById(owner.getIdFarm())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Fazenda vinculada ao produtor rural não encontrada para o ID: " + owner.getIdFarm()
                    ));
            if (!Objects.equals(idEnterprise, farm.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Produtor rural não tem permissão para visualizar lotes de outra empresa integradora"
                );
            }
        }

        return lotRepository.findAllByIdFarm(owner.getIdFarm())
                .stream()
                .map(LotResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca os detalhes de um lote específico pelo seu identificador único.
     *
     * @param id        identificador único do lote
     * @param principal usuário autenticado no JWT
     * @return DTO com os dados do lote
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não possuir acesso ao lote
     * @throws ResponseStatusException HTTP 404 se o lote não for encontrado
     */
    @Transactional(readOnly = true)
    public LotResponseDTO getLotById(Long id, UserPrincipal principal) {
        Lot lot = findLotByIdOrThrow(id);
        validateLotAccessPermission(lot, principal);
        return LotResponseDTO.fromEntity(lot);
    }

    /**
     * Atualiza dados pontuais ou realiza o fechamento do ciclo de um lote (PATCH /lots/{id}).
     *
     * @param id        identificador único do lote
     * @param request   dados para atualização parcial
     * @param principal usuário autenticado no JWT
     * @return DTO com o lote atualizado
     * @throws ResponseStatusException HTTP 400 se as novas regras de negócio forem violadas
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não possuir permissão para alterar o lote
     * @throws ResponseStatusException HTTP 404 se o lote não for encontrado
     */
    @Transactional
    public LotResponseDTO updateLot(Long id, LotUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        Lot lot = findLotByIdOrThrow(id);
        validateLotMutationPermission(lot, principal, "alterar");

        if (!request.hasUpdates()) {
            return LotResponseDTO.fromEntity(lot);
        }

        Integer newReceived = request.receivedChickens() != null ? request.receivedChickens() : lot.getReceivedChickens();
        Integer newDelivered = request.deliveredChickens() != null ? request.deliveredChickens() : lot.getDeliveredChickens();
        if (newDelivered > newReceived) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A quantidade de aves entregues não pode ser superior à quantidade de aves recebidas"
            );
        }

        lot.setReceivedChickens(newReceived);
        lot.setDeliveredChickens(newDelivered);

        if (request.deliveryDate() != null) {
            lot.setDeliveryDate(request.deliveryDate());
        }
        if (request.losts() != null) {
            lot.setLosts(request.losts());
        }
        if (request.cost() != null) {
            lot.setCost(request.cost());
        }

        Lot updatedLot = lotRepository.save(lot);
        return LotResponseDTO.fromEntity(updatedLot);
    }

    /**
     * Remove um lote de aves do sistema.
     *
     * @param id        identificador único do lote a ser excluído
     * @param principal usuário autenticado no JWT
     * @throws ResponseStatusException HTTP 401 se não autenticado
     * @throws ResponseStatusException HTTP 403 se o usuário não possuir permissão para remover o lote
     * @throws ResponseStatusException HTTP 404 se o lote não for encontrado
     * @throws ResponseStatusException HTTP 409 se houver restrições de chave estrangeira impedindo a exclusão
     */
    @Transactional
    public void deleteLot(Long id, UserPrincipal principal) {
        Lot lot = findLotByIdOrThrow(id);
        validateLotMutationPermission(lot, principal, "remover");

        try {
            lotRepository.delete(lot);
            lotRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível remover o lote pois existem registros vinculados a ele",
                    ex
            );
        }
    }

    /**
     * Valida permissão de leitura em um lote específico.
     */
    private void validateLotAccessPermission(Lot lot, UserPrincipal principal) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (RoleConstants.ADM.equals(role)) {
            return;
        }

        if (RoleConstants.COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            if (!Objects.equals(lot.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado a este lote"
                );
            }
            return;
        }

        if (RoleConstants.FARM_OWNER.equals(role)) {
            FarmOwner owner = getFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(lot.getIdFarm(), owner.getIdFarm())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado a este lote"
                );
            }
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para acessar este lote"
        );
    }

    /**
     * Valida permissão para mutação (edição ou exclusão) de um lote.
     */
    private void validateLotMutationPermission(Lot lot, UserPrincipal principal, String action) {
        ensureAuthenticated(principal);

        String role = principal.getRole();
        if (RoleConstants.ADM.equals(role)) {
            return;
        }

        if (RoleConstants.COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = getCompanyEmployeeOrThrow(principal.getId());
            if (!Objects.equals(lot.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado para " + action + " este lote"
                );
            }
            return;
        }

        if (RoleConstants.FARM_OWNER.equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Produtor rural não possui permissão para " + action + " lotes"
            );
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Perfil de usuário sem permissão para " + action + " lotes"
        );
    }

    /**
     * Garante que o usuário autenticado esteja presente.
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

    private Lot findLotByIdOrThrow(Long id) {
        if (id == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Lote não encontrado para o ID: null"
            );
        }
        return lotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Lote não encontrado para o ID: " + id
                ));
    }
}
