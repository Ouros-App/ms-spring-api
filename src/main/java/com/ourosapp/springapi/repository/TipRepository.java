package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para a entidade {@link Tip}.
 */
@Repository
public interface TipRepository extends JpaRepository<Tip, Long> {

    /**
     * Executa a stored procedure PostgreSQL 'create_tip' para inserção da dica técnica
     * com título, texto e imagem/banner, além da amarração atômica com a fazenda (farms_tips)
     * e categoria primária (tip_categories), retornando o ID numérico gerado da dica técnica.
     *
     * @param title      título da dica técnica
     * @param tip        texto descritivo da dica
     * @param fotoUrl    URL da imagem/banner ilustrativo (pode ser nulo)
     * @param idFarm     identificador da fazenda vinculada
     * @param idCategory identificador da categoria vinculada (pode ser nulo)
     * @return identificador único gerado da dica técnica criada
     */
    @Procedure(procedureName = "create_tip", outputParameterName = "p_tip_id")
    Integer callCreateTip(
            @Param("p_title") String title,
            @Param("p_tip") String tip,
            @Param("p_foto_url") String fotoUrl,
            @Param("p_id_farm") Integer idFarm,
            @Param("p_id_category") Integer idCategory
    );
}
