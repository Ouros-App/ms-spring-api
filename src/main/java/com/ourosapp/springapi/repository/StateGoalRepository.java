package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.StateGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link StateGoal}.
 */
@Repository
public interface StateGoalRepository extends JpaRepository<StateGoal, Long> {

    /**
     * Busca todas as metas estaduais vinculadas a uma fazenda específica.
     *
     * @param idFarm ID da fazenda
     * @return Lista de metas estaduais encontradas
     */
    List<StateGoal> findByIdFarm(Long idFarm);

    /**
     * Busca todas as metas estaduais vinculadas a uma lista de IDs de fazendas.
     *
     * @param idFarms Lista de IDs de fazendas
     * @return Lista de metas estaduais encontradas
     */
    List<StateGoal> findByIdFarmIn(List<Long> idFarms);
}
