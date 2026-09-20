package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.RegionGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para a entidade de associação {@link RegionGoal}.
 */
@Repository
public interface RegionGoalRepository extends JpaRepository<RegionGoal, Long> {

    List<RegionGoal> findByIdGoal(Long idGoal);

    List<RegionGoal> findByRegion(String region);

    Optional<RegionGoal> findByRegionAndIdGoal(String region, Long idGoal);

    boolean existsByRegionAndIdGoal(String region, Long idGoal);

    void deleteByIdGoal(Long idGoal);

    void deleteByRegionAndIdGoal(String region, Long idGoal);
}
