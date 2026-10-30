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

    @Test void devePaginarProducaoComDesempateEConverterProjecao() {
        var principal = user("ADM");
        var request = new PageQuery(1, 2, " L-7 ", "custo", "asc");
        var projection = mock(com.ourosapp.springapi.repository.projection.ProductionRegistryProjection.class);
        when(projection.getId()).thenReturn(7L);
        when(projection.getIdLote()).thenReturn("L-7");
        when(projection.getCusto()).thenReturn(50.0);
        var pageable = org.springframework.data.domain.PageRequest.of(1, 2,
                org.springframework.data.domain.Sort.by("custo").and(org.springframework.data.domain.Sort.by("id")));
        when(lotRepository.findProductionSummariesPaged(null, null, "%l-7%", pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(projection), pageable, 3));
        var result = service.production(null, null, request, principal);
        assertEquals(3, result.totalElements()); assertEquals(2, result.totalPages());
        assertEquals(1, result.page()); assertEquals(2, result.size());
        assertEquals(7L, result.content().get(0).id()); assertEquals(50.0, result.content().get(0).custo());
        verify(lotRepository).findProductionSummariesPaged(null, null, "%l-7%", pageable);
    }

    @Test void deveManterOrdenacaoPadraoSemDuplicarIdNaProducao() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        when(lotRepository.findProductionSummariesPaged(null, null, "%%", pageable))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));
        assertTrue(service.production(null, null, query, user("ADM")).content().isEmpty());
        verify(lotRepository).findProductionSummariesPaged(null, null, "%%", pageable);
    }

    @Test void deveConverterMetasIndividuaisDoEscopoDoProdutor() {
        var principal = user("FARM_OWNER");
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(FarmOwner.builder().idFarm(7L).build()));
        when(farmRepository.findById(7L)).thenReturn(Optional.of(Farm.builder().id(7L).idEnterprise(8L).build()));
        var goal = IndividualGoal.builder().id(9L).title("Água").idFarm(7L).build();
        when(repository.individualGoals(new PagedListingRepository.Scope(null, 7L), query))
                .thenReturn(PageResponseDTO.of(List.of(goal), 1, 0, 10));
        var result = service.individualGoals(7L, query, principal);
        assertEquals(9L, result.content().get(0).id()); assertEquals(1, result.totalElements());
    }

    @Test void deveCalcularSimulacaoApenasParaItensDaPaginaAutorizada() {
        var principal = user("COMPANY_EMPLOYEE");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(CompanyEmployee.builder().idEnterprise(8L).build()));
        when(farmRepository.findById(7L)).thenReturn(Optional.of(Farm.builder().id(7L).idEnterprise(8L).build()));
        var entity = new FarmSimulation(); entity.setId(9L);
        var dto = com.ourosapp.springapi.dto.farm.FarmSimulationResponseDTO.fromEntity(entity, null);
        when(repository.simulations(new PagedListingRepository.Scope(8L, 7L), query))
                .thenReturn(PageResponseDTO.of(List.of(entity), 25, 0, 10));
        when(simulationService.toResponseDTO(entity, principal)).thenReturn(dto);
        var result = service.simulations(7L, 8L, query, principal);
        assertEquals(List.of(dto), result.content()); assertEquals(25, result.totalElements());
        verify(simulationService).toResponseDTO(entity, principal);
    }

    @Test void deveRetornarAgregadoCompletoDasMetasEstaduais() {
        var expected = com.ourosapp.springapi.dto.stategoal.StateGoalPageResponseDTO.from(
                PageResponseDTO.of(List.of(), 21, 0, 10), Map.of("Pendente", 21L));
        when(repository.stateGoals(new PagedListingRepository.Scope(null, null), "Sul", query)).thenReturn(expected);
        assertSame(expected, service.stateGoals(null, "Sul", query, user("ADM")));
    }

    @Test void deveNegarFazendaDeOutraEmpresaAntesDeContar() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(CompanyEmployee.builder().idEnterprise(8L).build()));
        when(farmRepository.findById(7L)).thenReturn(Optional.of(Farm.builder().id(7L).idEnterprise(9L).build()));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.individualGoals(7L, query, user("COMPANY_EMPLOYEE"))).getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test void administradorPodeCombinarFiltrosDeFazendaEEmpresa() {
        when(farmRepository.findById(7L)).thenReturn(Optional.of(Farm.builder().id(7L).idEnterprise(9L).build()));
        var scope = new PagedListingRepository.Scope(8L, 7L);
        when(repository.simulations(scope, query)).thenReturn(PageResponseDTO.of(List.of(), 0, 0, 10));
        assertTrue(service.simulations(7L, 8L, query, user("ADM")).content().isEmpty());
        verify(repository).simulations(scope, query);
    }

    @Test void deveNegarEmpresaDiferenteDaFazendaDoProdutor() {
        when(ownerRepository.findById(1L)).thenReturn(Optional.of(FarmOwner.builder().idFarm(7L).build()));
        when(farmRepository.findById(7L)).thenReturn(Optional.of(Farm.builder().id(7L).idEnterprise(9L).build()));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.simulations(null, 8L, query, user("FARM_OWNER"))).getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test void deveRejeitarIdentidadesIncompletasOuNaoEncontradas() {
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> service.individualGoals(null, query, new UserPrincipal(null, "user@ouros.com", "", "ADM", List.of()))).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.individualGoals(null, query, user("COMPANY_EMPLOYEE"))).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.individualGoals(null, query, user("FARM_OWNER"))).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.individualGoals(7L, query, user("ADM"))).getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test void funcionarioSemEmpresaNaoPodeConsultarEscopoGlobal() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(CompanyEmployee.builder().build()));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.individualGoals(null, query, user("COMPANY_EMPLOYEE"))).getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test void deveNormalizarParametrosOpcionaisSemAlterarTermosLiterais() {
        var defaults = new PageQuery(0, 10, null, null, null);
        assertEquals("", defaults.search()); assertEquals("id", defaults.sort()); assertEquals("desc", defaults.direction());
        assertEquals("id desc", defaults.orderBy(Map.of("id", "id"), "id"));
        var blankSort = new PageQuery(0, 10, "  ÁGUA!  ", " ", "ASC");
        assertEquals("água!", blankSort.search()); assertEquals("id", blankSort.sort());
        assertEquals("%água!!%", blankSort.pattern());
    }

    @Test void deveNormalizarOrdenacaoERecusarDirecaoComComandos() {
        var normalized = new PageQuery(0, 10, "", " TITLE ", " ASC ");
        assertEquals("title", normalized.sort()); assertEquals("asc", normalized.direction());
        assertEquals("g.title asc, g.id asc", normalized.orderBy(Map.of("title", "g.title"), "g.id"));
        for (String direction : List.of("asc; delete from StateGoal", "desc OR 1=1", "asc desc", " ")) {
            assertEquals(400, assertThrows(ResponseStatusException.class,
                    () -> new PageQuery(0, 10, "", "id", direction)).getStatusCode().value());
        }
    }
}
