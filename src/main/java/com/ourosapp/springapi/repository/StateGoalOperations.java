package com.ourosapp.springapi.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Persistência transacional da meta e seus vínculos, sem rotinas externas de banco. */
public interface StateGoalOperations {
    Integer callCreateStateGoal(String title, String description, String type, String status,
            BigDecimal targetValue, LocalDateTime dateCreation, LocalDateTime dateEnd,
            Integer idFarm, String region);
}
