package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.common.*;
import com.ourosapp.springapi.dto.farm.FarmSimulationResponseDTO;
import com.ourosapp.springapi.dto.individualgoal.IndividualGoalResponseDTO;
import com.ourosapp.springapi.dto.lot.ProductionRegistryResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalPageResponseDTO;
import com.ourosapp.springapi.repository.*;
import com.ourosapp.springapi.repository.PagedListingRepository.Scope;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static com.ourosapp.springapi.constants.RoleConstants.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagedListingService {
    private final PagedListingRepository repository;
    private final LotRepository lotRepository;
    private final FarmRepository farmRepository;
    private final CompanyEmployeeRepository employeeRepository;
    private final FarmOwnerRepository ownerRepository;
    private final FarmSimulationService simulationService;

    public PageResponseDTO<ProductionRegistryResponseDTO> production(Long farmId, Long enterpriseId, PageQuery query, UserPrincipal principal) {
        Scope scope = scope(farmId, enterpriseId, principal);
        var fields = Map.of("id", "id", "hidrometro", "hidrometro", "custo", "custo", "entraram", "entraram",
                "sairam", "sairam", "galinhas_atuais", "galinhasAtuais", "data_emissao", "dataEmissao", "farm_name", "farmName");
        query.orderBy(fields, "id");
        Sort sort = Sort.by(Sort.Direction.fromString(query.direction()), fields.get(query.sort()));
        if (!query.sort().equals("id")) sort = sort.and(Sort.by(Sort.Direction.fromString(query.direction()), "id"));
        return PageResponseDTO.from(lotRepository.findProductionSummariesPaged(scope.enterpriseId(), scope.farmId(), query.pattern(),
                PageRequest.of(query.page(), query.size(), sort)).map(ProductionRegistryResponseDTO::fromProjection));
    }

    public PageResponseDTO<IndividualGoalResponseDTO> individualGoals(Long farmId, PageQuery query, UserPrincipal principal) {
        var result = repository.individualGoals(scope(farmId, null, principal), query);
        return PageResponseDTO.of(result.content().stream().map(IndividualGoalResponseDTO::fromEntity).toList(),
                result.totalElements(), query.page(), query.size());
    }

    public PageResponseDTO<FarmSimulationResponseDTO> simulations(Long farmId, Long enterpriseId, PageQuery query, UserPrincipal principal) {
        var result = repository.simulations(scope(farmId, enterpriseId, principal), query);
        return PageResponseDTO.of(result.content().stream().map(entity -> simulationService.toResponseDTO(entity, principal)).toList(),
                result.totalElements(), query.page(), query.size());
    }

    public StateGoalPageResponseDTO stateGoals(Long farmId, Long enterpriseId, String region, PageQuery query, UserPrincipal principal) {
        return repository.stateGoals(scope(farmId, enterpriseId, principal), region, query);
    }

    public StateGoalPageResponseDTO stateGoals(Long farmId, String region, PageQuery query, UserPrincipal principal) {
        return stateGoals(farmId, null, region, query, principal);
    }

    private Scope scope(Long farmId, Long enterpriseId, UserPrincipal principal) {
        if (principal == null || principal.getId() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        String role = principal.getRole();
        if (COMPANY_EMPLOYEE.equals(role)) {
            Long ownEnterprise = employeeRepository.findById(principal.getId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário logado não encontrado")).getIdEnterprise();
            if (ownEnterprise == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Funcionário sem empresa vinculada");
            if (enterpriseId != null && !enterpriseId.equals(ownEnterprise)) denied();
            enterpriseId = ownEnterprise;
        } else if (FARM_OWNER.equals(role)) {
            Long ownFarm = ownerRepository.findById(principal.getId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Produtor logado não encontrado")).getIdFarm();
            if (farmId != null && !farmId.equals(ownFarm)) denied();
            farmId = ownFarm == null ? -1L : ownFarm;
        } else if (!ADM.equals(role)) denied();
        if (farmId != null && farmId != -1L) {
            var farm = farmRepository.findById(farmId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada"));
            if (enterpriseId != null && !enterpriseId.equals(farm.getIdEnterprise()) && !ADM.equals(role)) denied();
        }
        return new Scope(enterpriseId, farmId);
    }

    private void denied() { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado para o escopo solicitado"); }
}
