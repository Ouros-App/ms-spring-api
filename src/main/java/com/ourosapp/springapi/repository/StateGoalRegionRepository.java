package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.StateGoalRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para a entidade de associação {@link StateGoalRegion}.
 */
@Repository
public interface StateGoalRegionRepository extends JpaRepository<StateGoalRegion, Long> {

    List<StateGoalRegion> findByIdGoal(Long idGoal);

    Optional<StateGoalRegion> findByIdGoalAndIdRegion(Long idGoal, Long idRegion);

    boolean existsByIdGoalAndIdRegion(Long idGoal, Long idRegion);

    void deleteByIdGoal(Long idGoal);

    void deleteByIdGoalAndIdRegion(Long idGoal, Long idRegion);
}
