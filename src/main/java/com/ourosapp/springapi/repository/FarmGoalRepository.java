package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.FarmGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para a entidade de associação {@link FarmGoal}.
 */
@Repository
public interface FarmGoalRepository extends JpaRepository<FarmGoal, Long> {

    List<FarmGoal> findByIdGoal(Long idGoal);

    List<FarmGoal> findByIdFarm(Long idFarm);

    Optional<FarmGoal> findByIdFarmAndIdGoal(Long idFarm, Long idGoal);

    boolean existsByIdFarmAndIdGoal(Long idFarm, Long idGoal);

    void deleteByIdGoal(Long idGoal);

    void deleteByIdFarmAndIdGoal(Long idFarm, Long idGoal);
}
