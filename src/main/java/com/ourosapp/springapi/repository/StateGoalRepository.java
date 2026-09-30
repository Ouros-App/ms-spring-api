package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.StateGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /**
     * Calcula o progresso percentual ponderado de metas da fazenda utilizando a função analítica PostgreSQL 'calculate_goals_progress'.
     *
     * @param farmId identificador único da fazenda
     * @return percentual ponderado de progresso de metas (0 a 100%)
     */
    @Query(value = "SELECT calculate_goals_progress(:farmId)", nativeQuery = true)
    BigDecimal getGoalsProgress(@Param("farmId") Long farmId);

    /**
     * Executa a stored procedure PostgreSQL 'create_state_goal' para inserção da meta estadual
     * e amarração atômica com a fazenda (farm_goals) e região (state_goal_regions).
     *
     * @param title        título da meta estadual
     * @param description  descrição detalhada da meta
     * @param type         tipo da meta
     * @param status       status da meta
     * @param targetValue  valor alvo quantitativo
     * @param dateCreation data de início / criação da meta
     * @param dateEnd      data de término prevista
     * @param idFarm       identificador da fazenda vinculada
     * @param idRegion     identificador da região vinculada
     */
    @Modifying
    @Transactional
    @Query(value = """
        CALL create_state_goal(
            :title, :description, :type, :status, :targetValue,
            :dateCreation, :dateEnd, :idFarm, :idRegion
        )
    """, nativeQuery = true)
    void callCreateStateGoal(
            @Param("title") String title,
            @Param("description") String description,
            @Param("type") String type,
            @Param("status") String status,
            @Param("targetValue") BigDecimal targetValue,
            @Param("dateCreation") LocalDate dateCreation,
            @Param("dateEnd") LocalDate dateEnd,
            @Param("idFarm") Long idFarm,
            @Param("idRegion") Long idRegion
    );
}
