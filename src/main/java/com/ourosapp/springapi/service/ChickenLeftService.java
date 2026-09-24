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
 * Serviço responsável pela gestão de saída e baixa de aves (abate/mortalidade/transferência)
 * e sincronização do saldo de aves do plantel na fazenda.
 */
@Service
@RequiredArgsConstructor
public class ChickenLeftService {

    private final ChickenLeftRepository chickenLeftRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;

    /**
     * Cadastra um novo registro de saída de aves e decrementa o saldo atual do plantel na fazenda.
     */
    @Transactional
    public ChickenLeftResponseDTO createChickenLeft(ChickenLeftRequestDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");
        validateAuthentication(principal);

        Long farmId = determineAuthorizedFarmId(request.idFarm(), principal);
        Farm targetFarm = lookupFarm(farmId);

        deductFlockQuantity(targetFarm, request.chickensCount());

        ChickenLeft chickenLeft = ChickenLeft.builder()
                .chickensCount(request.chickensCount())
                .exitDate(request.exitDate())
                .idFarm(farmId)
                .build();

        ChickenLeft saved = chickenLeftRepository.save(chickenLeft);
        return ChickenLeftResponseDTO.fromEntity(saved);
    }

    /**
     * Lista registros de saída de aves conforme escopo do perfil autenticado.
     */
    @Transactional(readOnly = true)
    public List<ChickenLeftResponseDTO> getChickenLeftsForUser(Long farmId, UserPrincipal principal) {
        validateAuthentication(principal);

        List<ChickenLeft> results = switch (principal.getRole()) {
            case ADM -> (farmId != null)
                    ? chickenLeftRepository.findAllByIdFarm(lookupFarm(farmId).getId())
                    : chickenLeftRepository.findAll();
            case COMPANY_EMPLOYEE -> queryForCorporateStaff(farmId, principal.getId());
            case FARM_OWNER -> queryForProducer(farmId, principal.getId());
            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Perfil de usuário sem permissão para listar registros de saída de aves"
            );
        };

        return results.stream().map(ChickenLeftResponseDTO::fromEntity).toList();
    }

    /**
     * Detalha um registro específico de saída de aves.
     */
    @Transactional(readOnly = true)
    public ChickenLeftResponseDTO getChickenLeftById(Long id, UserPrincipal principal) {
        ChickenLeft entry = lookupChickenLeft(id);
        authorizeFlockOperation(entry.getIdFarm(), principal, "acessar");
        return ChickenLeftResponseDTO.fromEntity(entry);
    }

    /**
     * Atualiza dados de saída de aves e reajusta o saldo remanescente do plantel na fazenda.
     */
    @Transactional
    public ChickenLeftResponseDTO updateChickenLeft(Long id, ChickenLeftUpdateDTO request, UserPrincipal principal) {
        Objects.requireNonNull(request, "O payload da requisição não pode ser nulo");

        ChickenLeft entry = lookupChickenLeft(id);
        authorizeFlockOperation(entry.getIdFarm(), principal, "alterar");

        if (!request.hasUpdates()) {
            return ChickenLeftResponseDTO.fromEntity(entry);
        }

        if (request.chickensCount() != null && !request.chickensCount().equals(entry.getChickensCount())) {
            int difference = request.chickensCount() - entry.getChickensCount();
            Farm farm = lookupFarm(entry.getIdFarm());
            int balance = farm.getChickensNow() != null ? farm.getChickensNow() : 0;

            if (difference > 0 && balance < difference) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                                "A quantidade adicional de aves de saída (%d) é maior que o saldo de aves atual na fazenda (%d)",
                                difference,
                                balance
                        )
                );
            }

            farm.setChickensNow(balance - difference);
            farmRepository.save(farm);
            entry.setChickensCount(request.chickensCount());
        }

        if (request.exitDate() != null) {
            entry.setExitDate(request.exitDate());
        }

        return ChickenLeftResponseDTO.fromEntity(chickenLeftRepository.save(entry));
    }

    /**
     * Remove um registro de saída de aves e estorna a quantidade baixada ao saldo do plantel.
     */
    @Transactional
    public void deleteChickenLeft(Long id, UserPrincipal principal) {
        ChickenLeft entry = lookupChickenLeft(id);
        authorizeFlockOperation(entry.getIdFarm(), principal, "remover");

        Farm farm = lookupFarm(entry.getIdFarm());
        int balance = farm.getChickensNow() != null ? farm.getChickensNow() : 0;
        farm.setChickensNow(balance + entry.getChickensCount());
        farmRepository.save(farm);

        chickenLeftRepository.delete(entry);
        chickenLeftRepository.flush();
    }

    private void deductFlockQuantity(Farm farm, int requestedDeduction) {
        int balance = farm.getChickensNow() != null ? farm.getChickensNow() : 0;
        if (balance < requestedDeduction) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(
                            "A quantidade de saída de aves (%d) é maior que o saldo de aves atual na fazenda (%d)",
                            requestedDeduction,
                            balance
                    )
            );
        }
        farm.setChickensNow(balance - requestedDeduction);
        farmRepository.save(farm);
    }

    private Long determineAuthorizedFarmId(Long explicitFarmId, UserPrincipal principal) {
        return switch (principal.getRole()) {
            case ADM -> {
                if (explicitFarmId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID da fazenda é obrigatório para cadastro por administrador");
                }
                lookupFarm(explicitFarmId);
                yield explicitFarmId;
            }
            case COMPANY_EMPLOYEE -> {
                if (explicitFarmId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID da fazenda é obrigatório para cadastro por funcionário");
                }
                CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário logado não encontrado para o ID: " + principal.getId()));
                Farm target = lookupFarm(explicitFarmId);
                if (!Objects.equals(target.getIdEnterprise(), employee.getIdEnterprise())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Funcionário não tem permissão para cadastrar registros em fazendas de outra empresa");
                }
                yield explicitFarmId;
            }
            case FARM_OWNER -> {
                FarmOwner owner = farmOwnerRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural logado não encontrado para o ID: " + principal.getId()));
                if (owner.getIdFarm() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produtor rural não possui fazenda vinculada para registrar saída de aves");
                }
                if (explicitFarmId != null && !Objects.equals(explicitFarmId, owner.getIdFarm())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Produtor rural não tem permissão para cadastrar registros em outra fazenda");
                }
                yield owner.getIdFarm();
            }
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão para cadastrar registros de saída de aves");
        };
    }

    private void authorizeFlockOperation(Long farmId, UserPrincipal principal, String verb) {
        validateAuthentication(principal);

        switch (principal.getRole()) {
            case ADM -> { /* Governança global possui acesso irrestrito */ }
            case COMPANY_EMPLOYEE -> {
                CompanyEmployee employee = companyEmployeeRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário logado não encontrado para o ID: " + principal.getId()));
                Farm target = lookupFarm(farmId);
                if (!Objects.equals(target.getIdEnterprise(), employee.getIdEnterprise())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para " + verb + " este registro de saída de aves");
                }
            }
            case FARM_OWNER -> {
                FarmOwner owner = farmOwnerRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural logado não encontrado para o ID: " + principal.getId()));
                if (!Objects.equals(farmId, owner.getIdFarm())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para " + verb + " este registro de saída de aves");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão para " + verb + " este registro de saída de aves");
        }
    }

    private List<ChickenLeft> queryForCorporateStaff(Long farmId, Long employeeId) {
        CompanyEmployee employee = companyEmployeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário logado não encontrado para o ID: " + employeeId));

        if (farmId != null) {
            Farm farm = lookupFarm(farmId);
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado aos registros de saída de aves de fazenda vinculada a outra empresa");
            }
            return chickenLeftRepository.findAllByIdFarm(farmId);
        }

        List<Long> enterpriseFarmIds = farmRepository.findAllByIdEnterprise(employee.getIdEnterprise())
                .stream().map(Farm::getId).toList();
        return enterpriseFarmIds.isEmpty() ? List.of() : chickenLeftRepository.findAllByIdFarmIn(enterpriseFarmIds);
    }

    private List<ChickenLeft> queryForProducer(Long farmId, Long ownerId) {
        FarmOwner owner = farmOwnerRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor rural logado não encontrado para o ID: " + ownerId));

        if (owner.getIdFarm() == null) {
            return List.of();
        }
        if (farmId != null && !Objects.equals(farmId, owner.getIdFarm())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado aos registros de saída de aves de outra fazenda");
        }
        return chickenLeftRepository.findAllByIdFarm(owner.getIdFarm());
    }

    private void validateAuthentication(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }

    private Farm lookupFarm(Long farmId) {
        if (farmId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: null");
        }
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada para o ID: " + farmId));
    }

    private ChickenLeft lookupChickenLeft(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de saída de aves não encontrado para o ID: null");
        }
        return chickenLeftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro de saída de aves não encontrado para o ID: " + id));
    }
}
