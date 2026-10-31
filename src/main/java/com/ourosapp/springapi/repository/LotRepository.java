package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Lot;
import com.ourosapp.springapi.repository.projection.ProductionRegistryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para a entidade {@link Lot}.
 * Fornece métodos de persistência e consultas customizadas por empresa e fazenda.
 */
@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {

    /**
     * Busca todos os lotes pertencentes a uma empresa integradora.
     *
     * @param idEnterprise identificador da empresa integradora
     * @return lista de lotes encontrados
     */
    List<Lot> findAllByIdEnterprise(Long idEnterprise);

    /**
     * Busca todos os lotes pertencentes a uma fazenda específica.
     *
     * @param idFarm identificador da fazenda
     * @return lista de lotes encontrados
     */
    List<Lot> findAllByIdFarm(Long idFarm);

    /**
     * Busca todos os lotes pertencentes a uma empresa e a uma fazenda específica.
     *
     * @param idEnterprise identificador da empresa integradora
     * @param idFarm       identificador da fazenda
     * @return lista de lotes encontrados
     */
    List<Lot> findAllByIdEnterpriseAndIdFarm(Long idEnterprise, Long idFarm);

    /**
     * Verifica se existe um lote com o ID informado pertencente à empresa especificada.
     *
     * @param id           identificador do lote
     * @param idEnterprise identificador da empresa
     * @return {@code true} se existir, caso contrário {@code false}
     */
    boolean existsByIdAndIdEnterprise(Long id, Long idEnterprise);

    /**
     * Verifica se existe um lote com o ID informado pertencente à fazenda especificada.
     *
     * @param id     identificador do lote
     * @param idFarm identificador da fazenda
     * @return {@code true} se existir, caso contrário {@code false}
     */
    boolean existsByIdAndIdFarm(Long id, Long idFarm);

    /**
     * Consulta consolidada de registros de produção avícola agrupando leituras de hidrômetro e saldo de aves.
     *
     * @param enterpriseId filtro opcional por empresa integradora
     * @param farmId       filtro opcional por fazenda
     * @return lista de projeções de resumo de produção
     */
    @Query(value = """
        SELECT
            l.id AS id,
            CONCAT('L-', l.id) AS idLote,
            COALESCE(l.delivery_date, CURRENT_DATE) AS dataEmissao,
            COALESCE(SUM(w.end_hydrometer - w.start_hydrometer), 0.0) AS hidrometro,
            COALESCE(l.cost, 0.0) AS custo,
            COALESCE(l.received_chickens, 0) AS entraram,
            COALESCE(l.delivered_chickens, 0) + COALESCE(l.losts, 0) AS sairam,
            GREATEST(COALESCE(l.received_chickens, 0) - COALESCE(l.delivered_chickens, 0) - COALESCE(l.losts, 0), 0) AS galinhasAtuais,
            f.id AS idFarm,
            f.name AS farmName
        FROM lots l
        INNER JOIN farms f ON l.id_farm = f.id
        LEFT JOIN water_registries w ON w.id_farm = f.id
        WHERE (:enterpriseId IS NULL OR l.id_enterprise = :enterpriseId)
          AND (:farmId IS NULL OR l.id_farm = :farmId)
        GROUP BY l.id, f.id, f.name
        ORDER BY l.id DESC
        """, nativeQuery = true)
    List<ProductionRegistryProjection> findProductionSummaries(
            @Param("enterpriseId") Long enterpriseId,
            @Param("farmId") Long farmId
    );
    @Query(value = """
        SELECT
            l.id AS id,
            CONCAT('L-', l.id) AS idLote,
            COALESCE(l.delivery_date, CURRENT_DATE) AS dataEmissao,
            COALESCE(SUM(w.end_hydrometer - w.start_hydrometer), 0.0) AS hidrometro,
            COALESCE(l.cost, 0.0) AS custo,
            COALESCE(l.received_chickens, 0) AS entraram,
            COALESCE(l.delivered_chickens, 0) + COALESCE(l.losts, 0) AS sairam,
            GREATEST(COALESCE(l.received_chickens, 0) - COALESCE(l.delivered_chickens, 0) - COALESCE(l.losts, 0), 0) AS galinhasAtuais,
            f.id AS idFarm,
            f.name AS farmName
        FROM lots l
        INNER JOIN farms f ON l.id_farm = f.id
        LEFT JOIN water_registries w ON w.id_farm = f.id
        WHERE l.id = :id
          AND (:enterpriseId IS NULL OR l.id_enterprise = :enterpriseId)
          AND (:farmId IS NULL OR l.id_farm = :farmId)
        GROUP BY l.id, f.id, f.name
        """, nativeQuery = true)
    java.util.Optional<ProductionRegistryProjection> findProductionSummaryById(
            @Param("id") Long id,
            @Param("enterpriseId") Long enterpriseId,
            @Param("farmId") Long farmId
    );
    @Query(value = """
        SELECT
            l.id AS id,
            CONCAT('L-', l.id) AS idLote,
            COALESCE(l.delivery_date, CURRENT_DATE) AS dataEmissao,
            COALESCE(SUM(w.end_hydrometer - w.start_hydrometer), 0.0) AS hidrometro,
            COALESCE(l.cost, 0.0) AS custo,
            COALESCE(l.received_chickens, 0) AS entraram,
            COALESCE(l.delivered_chickens, 0) + COALESCE(l.losts, 0) AS sairam,
            GREATEST(COALESCE(l.received_chickens, 0) - COALESCE(l.delivered_chickens, 0) - COALESCE(l.losts, 0), 0) AS galinhasAtuais,
            f.id AS idFarm,
            f.name AS farmName
        FROM lots l
        INNER JOIN farms f ON l.id_farm = f.id
        LEFT JOIN water_registries w ON w.id_farm = f.id
        WHERE (:enterpriseId IS NULL OR l.id_enterprise = :enterpriseId)
          AND (:farmId IS NULL OR l.id_farm = :farmId)
          AND (LOWER(CONCAT('L-', l.id)) LIKE :search ESCAPE '!' OR LOWER(f.name) LIKE :search ESCAPE '!')
        GROUP BY l.id, f.id, f.name
        """, countQuery = """
        SELECT COUNT(*) FROM lots l INNER JOIN farms f ON l.id_farm = f.id
        WHERE (:enterpriseId IS NULL OR l.id_enterprise = :enterpriseId)
          AND (:farmId IS NULL OR l.id_farm = :farmId)
          AND (LOWER(CONCAT('L-', l.id)) LIKE :search ESCAPE '!' OR LOWER(f.name) LIKE :search ESCAPE '!')
        """, nativeQuery = true)
    org.springframework.data.domain.Page<ProductionRegistryProjection> findProductionSummariesPaged(
            @Param("enterpriseId") Long enterpriseId,
            @Param("farmId") Long farmId,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );
}
