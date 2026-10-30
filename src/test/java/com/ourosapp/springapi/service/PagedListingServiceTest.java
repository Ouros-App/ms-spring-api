package com.ourosapp.springapi.service;

import com.ourosapp.springapi.dto.common.*;
import com.ourosapp.springapi.entity.*;
import com.ourosapp.springapi.repository.*;
import com.ourosapp.springapi.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class PagedListingServiceTest {
    @Mock PagedListingRepository repository;
    @Mock LotRepository lotRepository;
    @Mock FarmRepository farmRepository;
    @Mock CompanyEmployeeRepository employeeRepository;
    @Mock FarmOwnerRepository ownerRepository;
    @Mock FarmSimulationService simulationService;
    @InjectMocks PagedListingService service;
    private final PageQuery query = new PageQuery(0, 10, "", "id", "desc");
    private UserPrincipal user(String role) { return new UserPrincipal(1L, "user@ouros.com", "", role, List.of()); }

    @Test void deveNegarAcessoNaoAutenticadoEDesconhecido() {
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> service.individualGoals(null, query, null)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.individualGoals(null, query, user("UNKNOWN"))).getStatusCode().value());
        verifyNoInteractions(repository);
    }
    @Test void deveAplicarEmpresaAntesDeContar() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(CompanyEmployee.builder().idEnterprise(7L).build()));
        when(repository.individualGoals(new PagedListingRepository.Scope(7L, null), query)).thenReturn(PageResponseDTO.of(List.of(), 0, 0, 10));
        assertEquals(0, service.individualGoals(null, query, user("COMPANY_EMPLOYEE")).totalElements());
        verify(repository).individualGoals(new PagedListingRepository.Scope(7L, null), query);
    }
    @Test void deveNegarFiltroDeOutraEmpresaAntesDaConsulta() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(CompanyEmployee.builder().idEnterprise(7L).build()));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.simulations(null, 8L, query, user("COMPANY_EMPLOYEE"))).getStatusCode().value());
        verifyNoInteractions(repository);
    }
    @Test void deveNegarFazendaDeOutroProdutor() {
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(FarmOwner.builder().idFarm(7L).build()));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.stateGoals(8L, null, query, user("FARM_OWNER"))).getStatusCode().value());
        verifyNoInteractions(repository);
    }
    @Test void produtorSemFazendaNaoDeveConsultarTodasAsFazendas() {
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(FarmOwner.builder().build()));
        when(repository.individualGoals(new PagedListingRepository.Scope(null, -1L), query)).thenReturn(PageResponseDTO.of(List.of(), 0, 0, 10));
        assertTrue(service.individualGoals(null, query, user("FARM_OWNER")).content().isEmpty());
    }
    @Test void deveValidarLimitesEDirecaoDaPaginacao() {
        for (int[] values : List.of(new int[]{-1, 10}, new int[]{0, 0}, new int[]{0, 101}, new int[]{Integer.MAX_VALUE, 100})) {
            assertEquals(400, assertThrows(ResponseStatusException.class,
                    () -> new PageQuery(values[0], values[1], "", "id", "asc")).getStatusCode().value());
        }
        assertThrows(ResponseStatusException.class, () -> new PageQuery(0, 10, "", "id", "invalid"));
        assertThrows(ResponseStatusException.class, () -> query.orderBy(Map.of("title", "title"), "id"));
        assertEquals("%100!%!_%", new PageQuery(0, 100, "100%_", "id", "asc").pattern());
    }
}
