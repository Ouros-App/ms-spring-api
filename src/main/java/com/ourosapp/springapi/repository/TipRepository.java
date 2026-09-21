package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para a entidade {@link Tip}.
 */
@Repository
public interface TipRepository extends JpaRepository<Tip, Long> {

    List<Tip> findByIdFarm(Long idFarm);

    List<Tip> findByIdFarmIn(List<Long> farmIds);
}
