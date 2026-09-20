package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.IndividualGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link IndividualGoal}.
 */
@Repository
public interface IndividualGoalRepository extends JpaRepository<IndividualGoal, Long> {

    /**
     * Busca todas as metas individuais vinculadas a uma fazenda específica.
     *
     * @param idFarm ID da fazenda
     * @return Lista de metas individuais encontradas
     */
    List<IndividualGoal> findByIdFarm(Long idFarm);

    /**
     * Busca todas as metas individuais vinculadas a uma lista de IDs de fazendas.
     *
     * @param idFarms Lista de IDs de fazendas
     * @return Lista de metas individuais encontradas
     */
    List<IndividualGoal> findByIdFarmIn(List<Long> idFarms);
}
