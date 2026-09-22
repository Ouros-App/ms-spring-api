package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.FarmTip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para a entidade de associação {@link FarmTip}.
 */
@Repository
public interface FarmTipRepository extends JpaRepository<FarmTip, Long> {

    List<FarmTip> findByIdFarm(Long idFarm);

    List<FarmTip> findByIdFarmIn(List<Long> farmIds);

    List<FarmTip> findByIdTip(Long idTip);

    boolean existsByIdFarmAndIdTip(Long idFarm, Long idTip);

    void deleteByIdTip(Long idTip);

    void deleteByIdFarmAndIdTip(Long idFarm, Long idTip);
}
