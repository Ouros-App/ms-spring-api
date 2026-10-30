package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.dto.common.PageQuery;
import com.ourosapp.springapi.dto.common.PageResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalPageResponseDTO;
import com.ourosapp.springapi.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class PagedListingRepository {
    private final EntityManager entityManager;

    public record Scope(Long enterpriseId, Long farmId) { }

    private HibernateCriteriaBuilder builder() {
        return (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();
    }

    private List<Predicate> farmScope(CriteriaBuilder cb, AbstractQuery<?> query, From<?, ?> entity, Scope scope) {
        List<Predicate> predicates = new ArrayList<>();
        if (scope.farmId() != null) predicates.add(cb.equal(entity.get("idFarm"), scope.farmId()));
        if (scope.enterpriseId() != null) {
            var access = query.subquery(Long.class);
            var farm = access.from(Farm.class);
            access.select(farm.get("id")).where(cb.equal(farm.get("id"), entity.get("idFarm")),
                    cb.equal(farm.get("idEnterprise"), scope.enterpriseId()));
            predicates.add(cb.exists(access));
        }
        return predicates;
    }

    public PageResponseDTO<IndividualGoal> individualGoals(Scope scope, PageQuery page) {
        var cb = builder();
        var query = cb.createQuery(IndividualGoal.class);
        var goal = query.from(IndividualGoal.class);
        var predicates = farmScope(cb, query, goal, scope);
        predicates.add(cb.or(cb.like(cb.lower(goal.get("title")), page.pattern(), '!'),
                cb.like(cb.lower(goal.get("description")), page.pattern(), '!')));
        query.select(goal).where(predicates.toArray(Predicate[]::new));
        query.orderBy(orders(cb, page, Map.of("id", goal.get("id"), "title", goal.get("title"),
                "type", goal.get("type"), "status", goal.get("status"), "target_value", goal.get("targetValue")), goal.get("id")));
        return page(query, page);
    }

    public PageResponseDTO<FarmSimulation> simulations(Scope scope, PageQuery page) {
        var cb = builder();
        var query = cb.createQuery(FarmSimulation.class);
        var simulation = query.from(FarmSimulation.class);
        var predicates = farmScope(cb, query, simulation, scope);
        predicates.add(cb.like(cb.lower(simulation.get("idLot")), page.pattern(), '!'));
        if (scope.enterpriseId() != null) predicates.add(cb.equal(simulation.get("idEnterprise"), scope.enterpriseId()));
        query.select(simulation).where(predicates.toArray(Predicate[]::new));
        query.orderBy(orders(cb, page, Map.of("id", simulation.get("id"), "id_lot", simulation.get("idLot"),
                "current_cgi", simulation.get("currentCgi"), "target_cgi", simulation.get("targetCgi"),
                "water_price", simulation.get("waterPrice"), "energy_price", simulation.get("energyPrice"),
                "poultry_quantity", simulation.get("poultryQuantity"), "target_energy_kwh", simulation.get("targetEnergyKwh"),
                "created_at", simulation.get("createdAt")), simulation.get("id")));
        return page(query, page);
    }

    public StateGoalPageResponseDTO stateGoals(Scope scope, String region, PageQuery page) {
        var cb = builder();
        var query = cb.createTupleQuery();
        var state = stateSelection(cb, query, scope, region, page);
        query.multiselect(state.goal(), state.status(), state.region());
        query.orderBy(orders(cb, page, Map.of("id", state.goal().get("id"), "title", state.goal().get("title"),
                "type", state.goal().get("type"), "target_value", state.goal().get("targetValue"),
                "date_creation", state.goal().get("dateCreation"), "date_end", state.goal().get("dateEnd"),
                "region", state.region(), "status", state.status()), state.goal().get("id")));
        var result = page(query, page);
        var content = result.content().stream().map(row -> StateGoalResponseDTO.fromEntity(
                row.get(0, StateGoal.class), row.get(1, String.class), row.get(2, String.class))).toList();

        var summary = cb.createTupleQuery();
        var aggregate = stateSelection(cb, summary, scope, region, page);
        summary.multiselect(aggregate.status(), cb.count(aggregate.goal())).groupBy(aggregate.status());
        Map<String, Long> counts = new TreeMap<>();
        for (Tuple row : entityManager.createQuery(summary).getResultList()) {
            counts.put(row.get(0, String.class), row.get(1, Long.class));
        }
        return StateGoalPageResponseDTO.from(PageResponseDTO.of(content, result.totalElements(), page.page(), page.size()), counts);
    }

    private record StateSelection(JpaRoot<StateGoal> goal, Expression<String> status, Expression<String> region) { }

    private StateSelection stateSelection(HibernateCriteriaBuilder cb, JpaCriteriaQuery<Tuple> query,
                                           Scope scope, String region, PageQuery page) {
        var goal = query.from(StateGoal.class);
        // O modelo usa IDs escalares; os joins de entidades Criteria do Hibernate preservam os LEFT JOINs.
        // Escolhe o menor vínculo dentro do escopo autorizado, sem duplicar metas.
        var primaryLink = query.subquery(Long.class);
        var candidate = primaryLink.from(FarmGoal.class);
        var access = farmScope(cb, primaryLink, candidate, scope);
        access.add(cb.equal(candidate.get("idGoal"), goal.get("id")));
        primaryLink.select(cb.min(candidate.<Long>get("id"))).where(access.toArray(Predicate[]::new));

        var link = goal.join(FarmGoal.class, SqmJoinType.LEFT);
        link.on(cb.equal(link.get("idGoal"), goal.get("id")), cb.equal(link.get("id"), primaryLink));
        var farm = goal.join(Farm.class, SqmJoinType.LEFT);
        farm.on(cb.equal(farm.get("id"), link.get("idFarm")));

        var primaryRegion = query.subquery(Long.class);
        var regionCandidate = primaryRegion.from(RegionGoal.class);
        primaryRegion.select(cb.min(regionCandidate.<Long>get("id")))
                .where(cb.equal(regionCandidate.get("idGoal"), goal.get("id")));
        var regionLink = goal.join(RegionGoal.class, SqmJoinType.LEFT);
        regionLink.on(cb.equal(regionLink.get("idGoal"), goal.get("id")), cb.equal(regionLink.get("id"), primaryRegion));

        Expression<String> status = cb.coalesce(link.<String>get("status"), "Pendente");
        Expression<String> displayRegion = cb.coalesce(regionLink.<String>get("region"), farm.<String>get("region"));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.or(cb.like(cb.lower(goal.get("title")), page.pattern(), '!'),
                cb.like(cb.lower(cb.coalesce(displayRegion, "")), page.pattern(), '!')));
        if (scope.enterpriseId() != null || scope.farmId() != null) predicates.add(cb.isNotNull(link.get("id")));
        if (region != null && !region.isBlank()) {
            var matchingRegion = query.subquery(Long.class);
            var otherRegion = matchingRegion.from(RegionGoal.class);
            matchingRegion.select(otherRegion.get("id")).where(cb.equal(otherRegion.get("idGoal"), goal.get("id")),
                    cb.equal(cb.lower(otherRegion.get("region")), region.trim().toLowerCase(Locale.ROOT)));
            predicates.add(cb.or(cb.exists(matchingRegion), cb.and(cb.isNull(regionLink.get("id")),
                    cb.equal(cb.lower(farm.get("region")), region.trim().toLowerCase(Locale.ROOT)))));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return new StateSelection(goal, status, displayRegion);
    }

    private List<Order> orders(CriteriaBuilder cb, PageQuery page, Map<String, Expression<?>> fields, Expression<?> id) {
        Expression<?> selected = fields.get(page.sort());
        if (selected == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Campo de ordenação inválido. Permitidos: " + fields.keySet());
        boolean ascending = page.direction().equals("asc");
        List<Order> orders = new ArrayList<>();
        orders.add(ascending ? cb.asc(selected) : cb.desc(selected));
        if (!page.sort().equals("id")) orders.add(ascending ? cb.asc(id) : cb.desc(id));
        return orders;
    }

    private <T> PageResponseDTO<T> page(JpaCriteriaQuery<T> query, PageQuery page) {
        long total = entityManager.createQuery(query.createCountQuery()).getSingleResult();
        var content = entityManager.createQuery(query).setFirstResult((int) page.pageable().getOffset())
                .setMaxResults(page.size()).getResultList();
        return PageResponseDTO.of(content, total, page.page(), page.size());
    }
}
