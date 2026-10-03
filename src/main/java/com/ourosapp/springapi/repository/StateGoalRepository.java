package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.StateGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Query(value = "SELECT calculate_goals_progress(CAST(:farmId AS INTEGER))", nativeQuery = true)
    BigDecimal getGoalsProgress(@Param("farmId") Long farmId);

    /**
     * Executa a stored procedure PostgreSQL 'create_state_goal' para inserção da meta estadual
     * e amarração atômica com a fazenda (farm_goals) e região (state_goal_regions),
     * retornando o ID numérico gerado da meta estadual.
     *
     * @param title        título da meta estadual
     * @param description  descrição detalhada da meta
     * @param type         tipo da meta
     * @param status       status da meta
     * @param targetValue  valor alvo quantitativo
     * @param dateCreation data de início / criação da meta (TIMESTAMP)
     * @param dateEnd      data de término prevista (TIMESTAMP)
     * @param idFarm       identificador da fazenda vinculada (INTEGER)
     * @param region       nome da região vinculada
     * @return identificador único gerado da meta estadual criada
     */
    @Procedure(procedureName = "create_state_goal", outputParameterName = "p_goal_id")
    Integer callCreateStateGoal(
            @Param("p_title") String title,
            @Param("p_description") String description,
            @Param("p_type") String type,
            @Param("p_status") String status,
            @Param("p_target_value") BigDecimal targetValue,
            @Param("p_date_creation") LocalDateTime dateCreation,
            @Param("p_date_end") LocalDateTime dateEnd,
            @Param("p_id_farm") Integer idFarm,
            @Param("p_region") String region
    );
}
