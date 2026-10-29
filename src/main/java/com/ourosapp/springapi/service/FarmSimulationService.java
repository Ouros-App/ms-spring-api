package com.ourosapp.springapi.service;

import static com.ourosapp.springapi.constants.ErrorMessages.USER_NOT_AUTHENTICATED;
import static com.ourosapp.springapi.constants.RoleConstants.ADM;
import static com.ourosapp.springapi.constants.RoleConstants.COMPANY_EMPLOYEE;
import static com.ourosapp.springapi.constants.RoleConstants.FARM_OWNER;

import com.ourosapp.springapi.dto.farm.FarmRankingSimulationResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationRequestDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationResponseDTO;
import com.ourosapp.springapi.dto.farm.FarmSimulationUpdateDTO;
import com.ourosapp.springapi.entity.CompanyEmployee;
import com.ourosapp.springapi.entity.Farm;
import com.ourosapp.springapi.entity.FarmOwner;
import com.ourosapp.springapi.entity.FarmSimulation;
import com.ourosapp.springapi.repository.CompanyEmployeeRepository;
import com.ourosapp.springapi.repository.FarmOwnerRepository;
import com.ourosapp.springapi.repository.FarmRepository;
import com.ourosapp.springapi.repository.FarmSimulationRepository;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Serviço responsável pela gestão e persistência de cenários de simulação financeira e operacional
 * ({@link FarmSimulation}) no PostgreSQL, integrado à simulação de ganho de posições no Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FarmSimulationService {

    private final FarmSimulationRepository farmSimulationRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository companyEmployeeRepository;
    private final FarmOwnerRepository farmOwnerRepository;
    private final FarmRankingService farmRankingService;

    /**
     * Lista os cenários de simulação acessíveis ao usuário autenticado.
     *
     * @param idFarm       filtro opcional por fazenda
     * @param idEnterprise filtro opcional por empresa integradora (para ADM)
     * @param principal    usuário autenticado
     * @return lista de simulações salvas
     */
    @Transactional(readOnly = true)
    public List<FarmSimulationResponseDTO> getSimulations(Long idFarm, Long idEnterprise, UserPrincipal principal) {
        ensureAuthenticated(principal);
        String role = principal.getRole();

        List<FarmSimulation> entities;

        if (ADM.equals(role)) {
            if (idFarm != null && idEnterprise != null) {
                entities = farmSimulationRepository.findByIdEnterpriseAndIdFarmOrderByIdDesc(idEnterprise, idFarm);
            } else if (idFarm != null) {
                entities = farmSimulationRepository.findByIdFarmOrderByIdDesc(idFarm);
            } else if (idEnterprise != null) {
                entities = farmSimulationRepository.findByIdEnterpriseOrderByIdDesc(idEnterprise);
            } else {
                entities = farmSimulationRepository.findAll();
            }
        } else if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = findCompanyEmployeeOrThrow(principal.getId());
            if (idEnterprise != null && !Objects.equals(employee.getIdEnterprise(), idEnterprise)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acesso negado para consultar simulações de outra empresa integradora");
            }
            if (idFarm != null) {
                Farm farm = findFarmOrThrow(idFarm);
                if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Acesso negado para consultar simulações desta fazenda");
                }
                entities = farmSimulationRepository.findByIdEnterpriseAndIdFarmOrderByIdDesc(employee.getIdEnterprise(), idFarm);
            } else {
                entities = farmSimulationRepository.findByIdEnterpriseOrderByIdDesc(employee.getIdEnterprise());
            }
        } else if (FARM_OWNER.equals(role)) {
            FarmOwner owner = findFarmOwnerOrThrow(principal.getId());
            if (idFarm != null && !Objects.equals(owner.getIdFarm(), idFarm)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acesso negado para consultar simulações de outra fazenda");
            }
            entities = farmSimulationRepository.findByIdFarmOrderByIdDesc(owner.getIdFarm());
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão para consultar simulações");
        }

        return entities.stream()
                .map(entity -> toResponseDTO(entity, principal))
                .toList();
    }

    /**
     * Cria e persiste um novo cenário de simulação financeira e operacional.
     *
     * @param dto       dados do cenário de simulação
     * @param principal usuário autenticado
     * @return DTO com os dados persistidos e o cálculo de ranking simulado
     */
    @Transactional
    public FarmSimulationResponseDTO createSimulation(FarmSimulationRequestDTO dto, UserPrincipal principal) {
        ensureAuthenticated(principal);
        Objects.requireNonNull(dto, "Dados da simulação não podem ser nulos");

        Farm farm = findFarmOrThrow(dto.idFarm());
        validateFarmAccessPermission(farm, principal);

        LocalDateTime now = LocalDateTime.now();
        FarmSimulation entity = new FarmSimulation();
        entity.setIdFarm(farm.getId());
        entity.setIdEnterprise(farm.getIdEnterprise());
        entity.setIdLot(dto.idLot().trim());
        entity.setCurrentCgi(dto.currentCgi());
        entity.setTargetCgi(dto.targetCgi());
        entity.setWaterPrice(dto.waterPrice());
        entity.setEnergyPrice(dto.energyPrice());
        entity.setPoultryQuantity(dto.poultryQuantity());
        entity.setTargetEnergyKwh(dto.targetEnergyKwh());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        FarmSimulation saved = farmSimulationRepository.save(entity);
        return toResponseDTO(saved, principal);
    }

    /**
     * Atualiza parcialmente um cenário de simulação existente.
     *
     * @param id        identificador do cenário de simulação
     * @param dto       dados a atualizar
     * @param principal usuário autenticado
     * @return DTO atualizado com o novo cálculo de ranking simulado
     */
    @Transactional
    public FarmSimulationResponseDTO updateSimulation(Long id, FarmSimulationUpdateDTO dto, UserPrincipal principal) {
        ensureAuthenticated(principal);
        if (dto == null || !dto.hasUpdates()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ao menos um campo para atualização da simulação");
        }

        FarmSimulation entity = findSimulationOrThrow(id);
        validateSimulationPermission(entity, principal);

        if (dto.idFarm() != null && !Objects.equals(dto.idFarm(), entity.getIdFarm())) {
            Farm targetFarm = findFarmOrThrow(dto.idFarm());
            validateFarmAccessPermission(targetFarm, principal);
            entity.setIdFarm(targetFarm.getId());
            entity.setIdEnterprise(targetFarm.getIdEnterprise());
        }

        if (dto.idLot() != null && !dto.idLot().isBlank()) {
            entity.setIdLot(dto.idLot().trim());
        }
        if (dto.currentCgi() != null) {
            entity.setCurrentCgi(dto.currentCgi());
        }
        if (dto.targetCgi() != null) {
            entity.setTargetCgi(dto.targetCgi());
        }
        if (dto.waterPrice() != null) {
            entity.setWaterPrice(dto.waterPrice());
        }
        if (dto.energyPrice() != null) {
            entity.setEnergyPrice(dto.energyPrice());
        }
        if (dto.poultryQuantity() != null) {
            entity.setPoultryQuantity(dto.poultryQuantity());
        }
        if (dto.targetEnergyKwh() != null) {
            entity.setTargetEnergyKwh(dto.targetEnergyKwh());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        FarmSimulation saved = farmSimulationRepository.save(entity);
        return toResponseDTO(saved, principal);
    }

    /**
     * Remove um cenário de simulação existente.
     *
     * @param id        identificador do cenário de simulação
     * @param principal usuário autenticado
     */
    @Transactional
    public void deleteSimulation(Long id, UserPrincipal principal) {
        ensureAuthenticated(principal);
        FarmSimulation entity = findSimulationOrThrow(id);
        validateSimulationPermission(entity, principal);
        farmSimulationRepository.delete(entity);
    }

    // ==========================================
    // Métodos Auxiliares
    // ==========================================

    private FarmSimulationResponseDTO toResponseDTO(FarmSimulation entity, UserPrincipal principal) {
        FarmRankingSimulationResponseDTO simulatedRanking = null;
        try {
            if (entity.getIdFarm() != null && entity.getTargetCgi() != null && entity.getTargetCgi() > 0) {
                simulatedRanking = farmRankingService.simulateFarmRanking(
                        entity.getIdFarm(),
                        entity.getTargetCgi(),
                        principal
                );
            }
        } catch (Exception e) {
            log.debug("Não foi possível calcular ranking simulado em tempo real para fazenda {}: {}",
                    entity.getIdFarm(), e.getMessage());
        }
        return FarmSimulationResponseDTO.fromEntity(entity, simulatedRanking);
    }

    private void validateSimulationPermission(FarmSimulation simulation, UserPrincipal principal) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }
        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = findCompanyEmployeeOrThrow(principal.getId());
            if (!Objects.equals(simulation.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acesso negado para modificar simulação de outra empresa integradora");
            }
            return;
        }
        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = findFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(simulation.getIdFarm(), owner.getIdFarm())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acesso negado para modificar simulação de outra fazenda");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão");
    }

    private void validateFarmAccessPermission(Farm farm, UserPrincipal principal) {
        String role = principal.getRole();
        if (ADM.equals(role)) {
            return;
        }
        if (COMPANY_EMPLOYEE.equals(role)) {
            CompanyEmployee employee = findCompanyEmployeeOrThrow(principal.getId());
            if (!Objects.equals(farm.getIdEnterprise(), employee.getIdEnterprise())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acesso negado para criar ou alterar simulação nesta fazenda");
            }
            return;
        }
        if (FARM_OWNER.equals(role)) {
            FarmOwner owner = findFarmOwnerOrThrow(principal.getId());
            if (!Objects.equals(farm.getId(), owner.getIdFarm())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Acesso negado para criar ou alterar simulação nesta fazenda");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil de usuário sem permissão");
    }

    private FarmSimulation findSimulationOrThrow(Long id) {
        return farmSimulationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Simulação não encontrada para o ID: " + id));
    }

    private Farm findFarmOrThrow(Long idFarm) {
        return farmRepository.findById(idFarm)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Fazenda não encontrada para o ID: " + idFarm));
    }

    private CompanyEmployee findCompanyEmployeeOrThrow(Long id) {
        return companyEmployeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Funcionário não encontrado para o ID: " + id));
    }

    private FarmOwner findFarmOwnerOrThrow(Long id) {
        return farmOwnerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produtor rural não encontrado para o ID: " + id));
    }

    private void ensureAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, USER_NOT_AUTHENTICATED);
        }
    }
}
