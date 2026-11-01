package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.stategoal.*;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.repository.*;
import com.ourosapp.springapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Locale;
import java.util.Set;
import static com.ourosapp.springapi.constants.RoleConstants.*;

@Service
@RequiredArgsConstructor
public class StateGoalByStateService {
    private static final Set<String> STATES = Set.of("AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO");
    private final FarmRepository farmRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final CompanyEmployeeRepository employeeRepository;
    private final StateGoalRepository goalRepository;
    private final FarmGoalRepository farmGoalRepository;
    private final RegionGoalRepository regionRepository;
    private final StateGoalRegionRepository goalRegionRepository;

    @Transactional(readOnly = true)
    public StateGoalAudienceDTO preview(String state, Long enterpriseId, UserPrincipal principal) {
        Long scope = resolveEnterprise(enterpriseId, principal);
        String uf = normalizeState(state);
        return new StateGoalAudienceDTO(uf, scope, farmRepository.findParticipantsByState(uf, scope).size());
    }

    @Transactional
    public StateGoalByStateResponseDTO create(StateGoalByStateRequestDTO request, UserPrincipal principal) {
        Long scope = resolveEnterprise(request.idEnterprise(), principal);
        String uf = normalizeState(request.state());
        StateGoalRequestDTO data = request.goal();
        if (data.idFarm() != null || data.region() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe apenas a UF para selecionar as granjas, sem id_farm ou region");
        }
        if (!data.isDateRangeValid()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O prazo não pode ser anterior ao início");
        var farms = farmRepository.findParticipantsByState(uf, scope);
        if (farms.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma granja elegível nesta UF e empresa");
        StateGoal goal = goalRepository.save(StateGoal.builder().title(data.title()).description(data.description())
                .type(data.type()).targetValue(data.targetValue()).dateCreation(data.dateCreation()).dateEnd(data.dateEnd()).build());
        farmGoalRepository.saveAll(farms.stream().map(farm -> FarmGoal.builder()
                .idGoal(goal.getId()).idFarm(farm.getId()).status("Pendente").build()).toList());
        RegionGoal region = regionRepository.save(RegionGoal.builder().idGoal(goal.getId()).region(uf).build());
        goalRegionRepository.save(StateGoalRegion.builder().idGoal(goal.getId()).idRegion(region.getId()).build());
        return new StateGoalByStateResponseDTO(goal.getId(), uf, scope, farms.size());
    }

    private Long resolveEnterprise(Long requested, UserPrincipal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        if (COMPANY_EMPLOYEE.equals(principal.getRole())) {
            Long own = employeeRepository.findById(principal.getId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado")).getIdEnterprise();
            if (own == null || (requested != null && !requested.equals(own)))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito às granjas da sua empresa");
            return own;
        }
        if (!ADM.equals(principal.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas ADM e funcionário podem lançar metas por UF");
        if (requested == null || requested <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma empresa para lançar a meta estadual");
        if (!enterpriseRepository.existsById(requested)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada");
        return requested;
    }

    private String normalizeState(String state) {
        String uf = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        if (!STATES.contains(uf)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma UF brasileira válida");
        return uf;
    }
}
