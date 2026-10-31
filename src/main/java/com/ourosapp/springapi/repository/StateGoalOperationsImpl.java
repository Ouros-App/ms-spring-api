package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class StateGoalOperationsImpl implements StateGoalOperations {
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Integer callCreateStateGoal(String title, String description, String type, String status,
            BigDecimal targetValue, LocalDateTime dateCreation, LocalDateTime dateEnd,
            Integer idFarm, String region) {
        StateGoal goal = StateGoal.builder().title(title).description(description).type(type)
                .targetValue(targetValue).dateCreation(dateCreation.toLocalDate())
                .dateEnd(dateEnd == null ? null : dateEnd.toLocalDate()).build();
        entityManager.persist(goal);
        if (idFarm != null) {
            entityManager.persist(FarmGoal.builder().idGoal(goal.getId()).idFarm(idFarm.longValue())
                    .status(status == null || status.isBlank() ? "Pendente" : status).build());
        }
        if (region != null && !region.isBlank()) {
            RegionGoal link = RegionGoal.builder().idGoal(goal.getId()).region(region).build();
            entityManager.persist(link);
            entityManager.persist(StateGoalRegion.builder().idGoal(goal.getId()).idRegion(link.getId()).build());
        }
        return Math.toIntExact(goal.getId());
    }
}
