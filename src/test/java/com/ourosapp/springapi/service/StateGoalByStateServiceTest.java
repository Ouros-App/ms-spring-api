package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.stategoal.*;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.repository.*;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class StateGoalByStateServiceTest {
    @Mock FarmRepository farms;
    @Mock EnterpriseRepository enterprises;
    @Mock CompanyEmployeeRepository employees;
    @Mock StateGoalRepository goals;
    @Mock FarmGoalRepository links;
    @Mock RegionGoalRepository regions;
    @Mock StateGoalRegionRepository regionLinks;
    @InjectMocks StateGoalByStateService service;

    private UserPrincipal user(String role) { return new UserPrincipal(1L, "test@example.com", null, role, List.of()); }
    private StateGoalRequestDTO data() {
        return new StateGoalRequestDTO("Economizar água", null, "WATER_CONSUMPTION", null,
                BigDecimal.TEN, LocalDate.of(2026,1,1), LocalDate.of(2026,2,1), null, null);
    }
    private void employee() {
        when(employees.findById(1L)).thenReturn(Optional.of(CompanyEmployee.builder().id(1L).idEnterprise(2L).build()));
    }
    private void status(int expected, Runnable action) {
        assertEquals(expected, assertThrows(ResponseStatusException.class, action::run).getStatusCode().value());
    }

    @Test void deveCriarUmaMetaComTodosOsParticipantesEStatusIndependente() {
        employee();
        when(farms.findParticipantsByState("SP",2L)).thenReturn(List.of(Farm.builder().id(3L).build(),Farm.builder().id(4L).build()));
        when(goals.save(any())).thenAnswer(inv -> { StateGoal goal = inv.getArgument(0); goal.setId(10L); return goal; });
        when(regions.save(any())).thenAnswer(inv -> { RegionGoal region = inv.getArgument(0); region.setId(20L); return region; });
        var response = service.create(new StateGoalByStateRequestDTO(" sp ",null,data()),user("COMPANY_EMPLOYEE"));
        assertEquals(2,response.participantsCount());
        assertEquals(10L,response.id());
        verify(goals,times(1)).save(any());
        @SuppressWarnings("unchecked") ArgumentCaptor<Iterable<FarmGoal>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(links).saveAll(captor.capture());
        var iterator = captor.getValue().iterator();
        FarmGoal first = iterator.next(); FarmGoal second = iterator.next();
        assertEquals(10L, first.getIdGoal()); assertEquals(10L, second.getIdGoal());
        assertNotEquals(first.getIdFarm(),second.getIdFarm());
        first.setStatus("Concluída"); assertEquals("Pendente",second.getStatus());
        verify(regionLinks).save(any());
    }

    @Test void devePreverPublicoVazioSemGravar() {
        employee();
        assertEquals(0, service.preview("SP",2L,user("COMPANY_EMPLOYEE")).participantsCount());
        verifyNoInteractions(goals,links,regions,regionLinks);
        status(400,() -> service.create(new StateGoalByStateRequestDTO("SP",2L,data()),user("COMPANY_EMPLOYEE")));
        verifyNoInteractions(goals);
    }
    @Test void deveValidarEmpresaParaAdm() {
        status(400,() -> service.preview("SP",null,user("ADM")));
        status(400,() -> service.preview("SP",0L,user("ADM")));
        status(404,() -> service.preview("SP",9L,user("ADM")));
        when(enterprises.existsById(2L)).thenReturn(true);
        assertEquals(2L,service.preview("sp",2L,user("ADM")).idEnterprise());
        verifyNoInteractions(employees);
    }
    @Test void deveRejeitarOutraEmpresaFuncionarioSemEmpresaEFuncionarioAusente() {
        employee();
        status(403,() -> service.preview("SP",9L,user("COMPANY_EMPLOYEE")));
        when(employees.findById(1L)).thenReturn(Optional.of(CompanyEmployee.builder().id(1L).build()));
        status(403,() -> service.preview("SP",null,user("COMPANY_EMPLOYEE")));
        when(employees.findById(1L)).thenReturn(Optional.empty());
        status(404,() -> service.preview("SP",null,user("COMPANY_EMPLOYEE")));
        verifyNoInteractions(farms,goals);
    }
    @Test void deveRejeitarNaoAutenticadoProdutorEUfInvalida() {
        status(401,() -> service.preview("SP",null,null));
        status(403,() -> service.preview("SP",null,user("FARM_OWNER")));
        employee();
        status(400,() -> service.preview(null,null,user("COMPANY_EMPLOYEE")));
        status(400,() -> service.preview("XX",null,user("COMPANY_EMPLOYEE")));
        verifyNoInteractions(farms);
    }
    @Test void deveRejeitarSelecaoDeGranjaRegiaoOuPrazoInvalido() {
        employee();
        var data = data();
        for (var invalid : List.of(
                new StateGoalRequestDTO(data.title(),null,data.type(),null,BigDecimal.TEN,data.dateCreation(),data.dateEnd(),3L,null),
                new StateGoalRequestDTO(data.title(),null,data.type(),null,BigDecimal.TEN,data.dateCreation(),data.dateEnd(),null,"Sudeste"),
                new StateGoalRequestDTO(data.title(),null,data.type(),null,BigDecimal.TEN,data.dateCreation(),LocalDate.of(2025,1,1),null,null))) {
            status(400,() -> service.create(new StateGoalByStateRequestDTO("SP",null,invalid),user("COMPANY_EMPLOYEE")));
        }
        verifyNoInteractions(farms,goals,links);
    }
}
