package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repositório Spring Data JPA para a entidade {@link Tip}.
 */
@Repository
public interface TipRepository extends JpaRepository<Tip, Long> {

    /**
     * Executa a stored procedure PostgreSQL 'create_tip' para inserção da dica técnica
     * e amarração atômica com a fazenda (farms_tips) e categoria (tip_categories).
     *
     * @param tip        texto descritivo da dica
     * @param idFarm     identificador da fazenda vinculada
     * @param idCategory identificador da categoria vinculada (pode ser nulo)
     */
    @Modifying
    @Transactional
    @Query(value = "CALL create_tip(:tip, :idFarm, :idCategory)", nativeQuery = true)
    void callCreateTip(
            @Param("tip") String tip,
            @Param("idFarm") Long idFarm,
            @Param("idCategory") Long idCategory
    );
}
