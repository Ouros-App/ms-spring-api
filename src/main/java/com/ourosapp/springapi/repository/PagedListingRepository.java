package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.dto.common.PageQuery;
import com.ourosapp.springapi.dto.common.PageResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalResponseDTO;
import com.ourosapp.springapi.dto.stategoal.StateGoalPageResponseDTO;
import com.ourosapp.springapi.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class PagedListingRepository {
    private final EntityManager entityManager;

    public record Scope(Long enterpriseId, Long farmId) { }

    private String farmScope(Scope scope, String alias, Map<String, Object> params) {
        String where = "";
        if (scope.farmId() != null) {
            where += " and " + alias + ".idFarm = :farmId";
            params.put("farmId", scope.farmId());
        }
        if (scope.enterpriseId() != null) {
            where += " and exists (select f.id from Farm f where f.id = " + alias
                    + ".idFarm and f.idEnterprise = :enterpriseId)";
            params.put("enterpriseId", scope.enterpriseId());
        }
        return where;
    }

    public PageResponseDTO<IndividualGoal> individualGoals(Scope scope, PageQuery page) {
        Map<String, Object> params = new HashMap<>();
        params.put("search", page.pattern());
        String where = " from IndividualGoal g where (lower(g.title) like :search escape '!'"
                + " or lower(g.description) like :search escape '!')" + farmScope(scope, "g", params);
        String order = page.orderBy(Map.of("id", "g.id", "title", "g.title", "type", "g.type",
                "status", "g.status", "target_value", "g.targetValue"), "g.id");
        return page(IndividualGoal.class, "select g" + where, "select count(g)" + where, params, page, order);
    }

    public PageResponseDTO<FarmSimulation> simulations(Scope scope, PageQuery page) {
        Map<String, Object> params = new HashMap<>();
        params.put("search", page.pattern());
        String where = " from FarmSimulation s where lower(s.idLot) like :search escape '!'"
                + farmScope(scope, "s", params);
        if (scope.enterpriseId() != null) where += " and s.idEnterprise = :enterpriseId";
        String order = page.orderBy(Map.of("id", "s.id", "id_lot", "s.idLot", "current_cgi", "s.currentCgi",
                "target_cgi", "s.targetCgi", "water_price", "s.waterPrice", "energy_price", "s.energyPrice",
                "poultry_quantity", "s.poultryQuantity", "target_energy_kwh", "s.targetEnergyKwh",
                "created_at", "s.createdAt"), "s.id");
        return page(FarmSimulation.class, "select s" + where, "select count(s)" + where, params, page, order);
    }

    public StateGoalPageResponseDTO stateGoals(Scope scope, String region, PageQuery page) {
        Map<String, Object> params = new HashMap<>();
        params.put("search", page.pattern());
        // Escolhe um vínculo determinístico DENTRO do escopo, evitando duplicação de metas.
        String linkScope = farmScope(scope, "candidate", params);
        String from = " from StateGoal g left join FarmGoal fg on fg.idGoal = g.id"
                + " and fg.id = (select min(candidate.id) from FarmGoal candidate where candidate.idGoal = g.id"
                + linkScope + ") left join Farm f on f.id = fg.idFarm"
                + " left join RegionGoal rg on rg.idGoal = g.id and rg.id ="
                + " (select min(r.id) from RegionGoal r where r.idGoal = g.id)";
        String where = " where (lower(g.title) like :search escape '!' or lower(coalesce(rg.region, f.region, '')) like :search escape '!')";
        if (scope.enterpriseId() != null || scope.farmId() != null) where += " and fg.id is not null";
        if (region != null && !region.isBlank()) {
            params.put("region", region.trim());
            where += " and (exists (select r.id from RegionGoal r where r.idGoal = g.id and lower(r.region) = lower(:region))"
                    + " or (rg.id is null and lower(f.region) = lower(:region)))";
        }
        String order = page.orderBy(Map.of("id", "g.id", "title", "g.title", "type", "g.type",
                "target_value", "g.targetValue", "date_creation", "g.dateCreation", "date_end", "g.dateEnd",
                "region", "coalesce(rg.region, f.region)", "status", "coalesce(fg.status, 'Pendente')"), "g.id");
        long total = ((Number) bind(entityManager.createQuery("select count(g)" + from + where), params).getSingleResult()).longValue();
        List<?> rows = bind(entityManager.createQuery("select g, coalesce(fg.status, 'Pendente'), coalesce(rg.region, f.region)" + from + where + " order by " + order), params)
                .setFirstResult((int) page.pageable().getOffset()).setMaxResults(page.size()).getResultList();
        List<StateGoalResponseDTO> content = rows.stream().map(row -> {
            Object[] values = (Object[]) row;
            return StateGoalResponseDTO.fromEntity((StateGoal) values[0], (String) values[1], (String) values[2]);
        }).toList();
        List<?> aggregates = bind(entityManager.createQuery("select coalesce(fg.status, 'Pendente'), count(g)" + from + where
                + " group by coalesce(fg.status, 'Pendente')"), params).getResultList();
        Map<String, Long> counts = new TreeMap<>();
        for (Object row : aggregates) {
            Object[] values = (Object[]) row;
            counts.put((String) values[0], ((Number) values[1]).longValue());
        }
        return StateGoalPageResponseDTO.from(PageResponseDTO.of(content, total, page.page(), page.size()), counts);
    }

    private <T> PageResponseDTO<T> page(Class<T> type, String select, String count, Map<String, Object> params,
                                       PageQuery page, String order) {
        var query = entityManager.createQuery(select + " order by " + order, type);
        params.forEach(query::setParameter);
        long total = ((Number) bind(entityManager.createQuery(count), params).getSingleResult()).longValue();
        return PageResponseDTO.of(query.setFirstResult((int) page.pageable().getOffset()).setMaxResults(page.size())
                .getResultList(), total, page.page(), page.size());
    }

    private Query bind(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
        return query;
    }
}
