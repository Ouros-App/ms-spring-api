package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.dto.common.PageQuery;
import com.ourosapp.springapi.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.show-sql=false")
@Import(PagedListingRepository.class)
class PagedListingRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired PagedListingRepository repository;
    @Autowired LotRepository lots;

    private Farm farm(long enterprise, String region) {
        return em.persist(Farm.builder().name("Granja " + enterprise).idEnterprise(enterprise).idAddress(1L)
                .areaProperty(BigDecimal.ONE).region(region).build());
    }
    private PageQuery query(int page, int size, String search, String sort) {
        return new PageQuery(page, size, search, sort, "asc");
    }
    private StateGoal goal(Farm farm, String title, String status) {
        var goal = em.persist(StateGoal.builder().title(title).description("Descrição").type("Água")
                .targetValue(BigDecimal.TEN).dateCreation(LocalDate.of(2026, 1, 1)).build());
        em.persist(FarmGoal.builder().idGoal(goal.getId()).idFarm(farm.getId()).status(status).build());
        return goal;
    }

    @Test void devePaginarMetasSemDuplicarVinculosEAgregarTodoEscopo() {
        Farm a = farm(1, "Sul"), b = farm(1, "Norte"), external = farm(2, "Sul");
        var first = goal(a, "Água", "CONCLUIDA");
        em.persist(FarmGoal.builder().idGoal(first.getId()).idFarm(b.getId()).status("Pendente").build());
        goal(a, "Água", "Pendente"); goal(a, "Água", "CONCLUIDA"); goal(external, "Água", "Pendente");
        var scope = new PagedListingRepository.Scope(1L, null);
        var page0 = repository.stateGoals(scope, "sul", query(0, 1, "Água", "title"));
        var page1 = repository.stateGoals(scope, "Sul", query(1, 1, "Água", "title"));
        var last = repository.stateGoals(scope, "Sul", query(2, 1, "Água", "title"));
        var empty = repository.stateGoals(scope, "Sul", query(3, 1, "Água", "title"));
        assertEquals(3, page0.totalElements()); assertEquals(3, page0.totalPages());
        assertNotEquals(page0.content().get(0).id(), page1.content().get(0).id());
        assertNotEquals(page1.content().get(0).id(), last.content().get(0).id());
        assertEquals(Map.of("CONCLUIDA", 2L, "Pendente", 1L), page0.statusCounts());
        assertEquals(page0.statusCounts(), empty.statusCounts()); assertTrue(empty.content().isEmpty());
        assertEquals(0, repository.stateGoals(scope, "Sudeste", query(0, 10, "", "id")).totalElements());
    }

    @Test void deveFiltrarRegiaoAssociadaAntesDaPaginacao() {
        Farm a = farm(1, "Sul"); StateGoal g = goal(a, "Meta", "Pendente");
        em.persist(RegionGoal.builder().idGoal(g.getId()).region("Nordeste").build());
        var scope = new PagedListingRepository.Scope(null, a.getId());
        assertEquals(0, repository.stateGoals(scope, "Sul", query(0, 10, "", "id")).totalElements());
        assertEquals(1, repository.stateGoals(scope, "nordeste", query(0, 10, "", "id")).totalElements());
    }

    @Test void deveBuscarPaginarEIsolarMetasIndividuaisComDesempatePorId() {
        Farm a = farm(1, "Sul"), b = farm(2, "Sul");
        for (int i = 0; i < 4; i++) em.persist(IndividualGoal.builder().title("Consumo")
                .description(i == 3 ? "100%_" : "Água").type("Água").status("Pendente")
                .targetValue(BigDecimal.TEN).idFarm(i == 2 ? b.getId() : a.getId()).build());
        var scope = new PagedListingRepository.Scope(1L, null);
        var p0 = repository.individualGoals(scope, query(0, 1, "ÁGUA", "title"));
        var p1 = repository.individualGoals(scope, query(1, 1, "água", "title"));
        assertEquals(2, p0.totalElements()); assertNotEquals(p0.content().get(0).getId(), p1.content().get(0).getId());
        assertEquals(1, repository.individualGoals(scope, query(0, 10, "100%_", "id")).totalElements());
        assertTrue(repository.individualGoals(scope, query(5, 10, "", "id")).content().isEmpty());
    }

    @Test void devePaginarSimulacoesAposBuscaEEscopo() {
        Farm a = farm(1, "Sul"), b = farm(2, "Sul");
        for (Farm f : List.of(a, a, a, b)) {
            FarmSimulation s = new FarmSimulation(); s.setIdFarm(f.getId()); s.setIdEnterprise(f.getIdEnterprise());
            s.setIdLot("L-77"); s.setCurrentCgi(1.0); s.setTargetCgi(0.8); s.setWaterPrice(2.0);
            s.setEnergyPrice(3.0); s.setPoultryQuantity(100); s.setTargetEnergyKwh(50.0); em.persist(s);
        }
        var result = repository.simulations(new PagedListingRepository.Scope(1L, null), query(1, 2, "l-77", "current_cgi"));
        assertEquals(3, result.totalElements()); assertEquals(2, result.totalPages()); assertEquals(1, result.content().size());
        assertEquals(a.getId(), result.content().get(0).getIdFarm());
    }

    @Test void deveContarLotesConsolidadosSemMultiplicarPorLeituras() {
        Farm a = farm(1, "Sul");
        for (int i = 0; i < 3; i++) em.persist(Lot.builder().idFarm(a.getId()).idEnterprise(1L)
                .receivedChickens(100).deliveredChickens(10).losts(2).cost(50.0).build());
        for (int i = 0; i < 2; i++) em.persist(WaterRegistry.builder().idFarm(a.getId())
                .registrationDate(LocalDate.of(2026, 1, i + 1)).startHydrometer(BigDecimal.TEN)
                .endHydrometer(BigDecimal.valueOf(15)).build());
        em.flush();
        var pageable = org.springframework.data.domain.PageRequest.of(1, 2,
                org.springframework.data.domain.Sort.by("id"));
        var result = lots.findProductionSummariesPaged(1L, a.getId(), "%l-%", pageable);
        assertEquals(3, result.getTotalElements()); assertEquals(1, result.getContent().size());
        assertEquals(88, result.getContent().get(0).getGalinhasAtuais());
        assertEquals(10.0, result.getContent().get(0).getHidrometro());
        assertEquals(0, lots.findProductionSummariesPaged(2L, null, "%", pageable).getTotalElements());
    }

    @Test void deveTratarTentativasDeInjecaoComoTextoSemAmpliarEscopo() {
        Farm own = farm(1, "Sul"), other = farm(2, "Sul");
        String payload = "' OR 1=1 --";
        var target = em.persist(IndividualGoal.builder().title("Texto " + payload).description("Descrição")
                .type("Água").status("Pendente").targetValue(BigDecimal.TEN).idFarm(own.getId()).build());
        em.persist(IndividualGoal.builder().title("Meta comum").description("Descrição").type("Água")
                .status("Pendente").targetValue(BigDecimal.TEN).idFarm(own.getId()).build());
        em.persist(IndividualGoal.builder().title("Texto " + payload).description("Descrição").type("Água")
                .status("Pendente").targetValue(BigDecimal.TEN).idFarm(other.getId()).build());
        var scope = new PagedListingRepository.Scope(1L, null);
        var result = repository.individualGoals(scope, query(0, 10, payload, "id"));
        assertEquals(1, result.totalElements()); assertEquals(target.getId(), result.content().get(0).getId());
        var state = goal(own, "Texto " + payload, "Pendente");
        goal(own, "Meta comum", "CONCLUIDA"); goal(other, "Texto " + payload, "Pendente");
        var goals = repository.stateGoals(scope, null, query(0, 10, payload, "id"));
        assertEquals(1, goals.totalElements()); assertEquals(state.getId(), goals.content().get(0).id());
        assertEquals(Map.of("Pendente", 1L), goals.statusCounts());
        assertEquals(0, repository.stateGoals(scope, "Sul" + payload, query(0, 10, "", "id")).totalElements());
        for (String sort : List.of("title desc; delete from StateGoal", "id OR 1=1", "unknown")) {
            var invalid = query(0, 10, "", sort);
            assertEquals(400, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> repository.individualGoals(scope, invalid)).getStatusCode().value());
            assertEquals(400, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> repository.stateGoals(scope, null, invalid)).getStatusCode().value());
            assertEquals(400, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> repository.simulations(scope, invalid)).getStatusCode().value());
        }
        assertEquals(3L, em.getEntityManager().createQuery("select count(g) from StateGoal g", Long.class).getSingleResult());
    }

    @Test void consultaGlobalDevePreservarMetasSemVinculoEOrdenacaoDescendente() {
        Farm own = farm(1, "Sul");
        StateGoal linked = goal(own, "Meta A", "CONCLUIDA");
        StateGoal unlinked = em.persist(StateGoal.builder().title("Meta Z").description("Descrição").type("Água")
                .targetValue(BigDecimal.TEN).dateCreation(LocalDate.of(2026, 1, 1)).build());
        var scope = new PagedListingRepository.Scope(null, null);
        var request = new PageQuery(0, 10, "", "title", "desc");
        var result = repository.stateGoals(scope, null, request);
        assertEquals(2, result.totalElements());
        assertEquals(List.of(unlinked.getId(), linked.getId()), result.content().stream().map(g -> g.id()).toList());
        assertEquals(Map.of("CONCLUIDA", 1L, "Pendente", 1L), result.statusCounts());
        assertEquals(result, repository.stateGoals(scope, "  ", request));
    }

    @Test void deveBuscarSimulacoesPorTextoLiteralEPaginarSemFiltroDeEmpresa() {
        Farm own = farm(1, "Sul"), other = farm(2, "Norte");
        String payload = "' OR 1=1 --";
        for (Farm f : List.of(own, own, other)) {
            FarmSimulation s = new FarmSimulation(); s.setIdFarm(f.getId()); s.setIdEnterprise(f.getIdEnterprise());
            s.setIdLot(payload); s.setCurrentCgi(1.0); s.setTargetCgi(0.8); s.setWaterPrice(2.0);
            s.setEnergyPrice(3.0); s.setPoultryQuantity(100); s.setTargetEnergyKwh(50.0); em.persist(s);
        }
        var request = new PageQuery(0, 1, payload, "current_cgi", "desc");
        var first = repository.simulations(new PagedListingRepository.Scope(null, own.getId()), request);
        var next = repository.simulations(new PagedListingRepository.Scope(null, own.getId()),
                new PageQuery(1, 1, payload, "current_cgi", "desc"));
        assertEquals(2, first.totalElements()); assertEquals(own.getId(), first.content().get(0).getIdFarm());
        assertTrue(first.content().get(0).getId() > next.content().get(0).getId());
        assertEquals(3, repository.simulations(new PagedListingRepository.Scope(null, null),
                new PageQuery(0, 10, "", "id", "desc")).totalElements());
    }
}
