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

    /**
     * Calcula a proporção de vínculos concluídos da fazenda, sem funções externas.
     *
     * @param farmId identificador único da fazenda
     * @return percentual de progresso de metas (0 a 100%)
     */
    @Query(value = "SELECT COALESCE(100.0 * SUM(CASE WHEN UPPER(status) IN ('CONCLUIDA', 'CONCLUÍDA', 'COMPLETED', 'COMPLETED_SUCCESSFULLY', 'ACHIEVED') THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 0) FROM farm_goals WHERE id_farm = :farmId", nativeQuery = true)
    BigDecimal getGoalsProgress(@Param("farmId") Long farmId);

}
