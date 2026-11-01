package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.StateGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link StateGoal}.
 */
@Repository
public interface StateGoalRepository extends JpaRepository<StateGoal, Long>, StateGoalOperations {

    /**
     * Busca todas as metas estaduais vinculadas a uma fazenda específica através da tabela associativa.
     *
     * @param idFarm ID da fazenda
     * @return Lista de metas estaduais encontradas
     */
    @Query("SELECT g FROM StateGoal g WHERE g.id IN (SELECT fg.idGoal FROM FarmGoal fg WHERE fg.idFarm = :idFarm)")
    List<StateGoal> findByIdFarm(@Param("idFarm") Long idFarm);

    /**
     * Busca todas as metas estaduais vinculadas a uma lista de IDs de fazendas através da tabela associativa.
     *
     * @param idFarms Lista de IDs de fazendas
     * @return Lista de metas estaduais encontradas
     */
    @Query("SELECT g FROM StateGoal g WHERE g.id IN (SELECT fg.idGoal FROM FarmGoal fg WHERE fg.idFarm IN :idFarms)")
    List<StateGoal> findByIdFarmIn(@Param("idFarms") List<Long> idFarms);

    /** Replica a regra de funcoes_consumo_metas.sql para metas individuais e estaduais. */
    @Query(value = """
            WITH goals AS (
                SELECT status FROM individual_goals WHERE id_farm = :farmId
                UNION ALL
                SELECT status FROM farm_goals WHERE id_farm = :farmId
            )
            SELECT COALESCE(ROUND(AVG(CASE UPPER(status)
                WHEN 'CONCLUÍDA' THEN 100
                WHEN 'CONCLUIDA' THEN 100
                WHEN 'COMPLETED' THEN 100
                WHEN 'ACHIEVED' THEN 100
                WHEN 'FINALIZADA' THEN 100
                WHEN 'EM ANDAMENTO' THEN 50
                WHEN 'IN_PROGRESS' THEN 50
                WHEN 'PENDENTE' THEN 0
                WHEN 'PENDING' THEN 0
                ELSE 0 END), 2), 0)
            FROM goals
            """, nativeQuery = true)
    BigDecimal getGoalsProgress(@Param("farmId") Long farmId);
}
