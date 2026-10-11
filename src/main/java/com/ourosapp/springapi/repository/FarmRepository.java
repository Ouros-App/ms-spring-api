package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link Farm}.
 */
@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {

    /**
     * Busca todas as fazendas vinculadas a uma empresa integradora específica.
     *
     * @param idEnterprise identificador único da empresa integradora
     * @return lista de fazendas vinculadas à empresa
     */
    List<Farm> findAllByIdEnterprise(Long idEnterprise);

    /**
     * Busca todas as fazendas associadas a um determinado endereço.
     *
     * @param idAddress identificador único do endereço
     * @return lista de fazendas associadas ao endereço
     */
    List<Farm> findAllByIdAddress(Long idAddress);

    /**
     * Calcula o ranqueamento, indicadores (CAA, CEA, CGI), percentil e medalhas para as fazendas de uma integradora.
     *
     * @param idEnterprise identificador da empresa integradora
     * @return lista de projeções ordenadas por classificação
     */
    @org.springframework.data.jpa.repository.Query(value = """
        WITH water_agg AS (
            SELECT id_farm, COALESCE(SUM(end_hydrometer - start_hydrometer), 0) AS total_water
            FROM water_registries
            GROUP BY id_farm
        ),
        energy_agg AS (
            SELECT id_farm, COALESCE(SUM(energy_consumption), 0) AS total_energy
            FROM energy_registries
            GROUP BY id_farm
        ),
        farm_base AS (
            SELECT
                f.id AS farm_id,
                f.name AS farm_name,
                f.region AS region,
                NULL::INTEGER AS poultry_capacity,
                f.chickens_now AS chickens_now,
                f.id_enterprise AS id_enterprise,
                CASE
                    WHEN COALESCE(f.chickens_now, 0) > 0 THEN (COALESCE(w.total_water, 0) * 1.0 / f.chickens_now)
                    ELSE 0.0
                END AS caa,
                CASE
                    WHEN COALESCE(f.chickens_now, 0) > 0 THEN (COALESCE(e.total_energy, 0) * 1.0 / f.chickens_now)
                    ELSE 0.0
                END AS cea
            FROM farms f
            LEFT JOIN water_agg w ON w.id_farm = f.id
            LEFT JOIN energy_agg e ON e.id_farm = f.id
            WHERE f.id_enterprise = :idEnterprise
        ),
        farm_cgi AS (
            SELECT
                b.*,
                (b.caa * 0.7 + b.cea * 0.3) AS cgi
            FROM farm_base b
        ),
        farm_ranked AS (
            SELECT
                c.*,
                ROW_NUMBER() OVER (PARTITION BY c.id_enterprise ORDER BY c.cgi ASC, c.farm_id ASC) AS rank_pos,
                COUNT(*) OVER (PARTITION BY c.id_enterprise) AS total_count
            FROM farm_cgi c
        ),
        farm_percentile AS (
            SELECT
                r.*,
                CASE
                    WHEN r.total_count = 1 THEN 0.0
                    ELSE (r.rank_pos * 100.0 / r.total_count)
                END AS percentile_pct
            FROM farm_ranked r
        )
        SELECT
            p.farm_id AS farm_id,
            p.farm_name AS farm_name,
            p.region AS region,
            p.poultry_capacity AS poultry_capacity,
            p.chickens_now AS chickens_now,
            p.id_enterprise AS id_enterprise,
            ROUND(CAST(p.caa AS NUMERIC), 4) AS caa,
            ROUND(CAST(p.cea AS NUMERIC), 4) AS cea,
            ROUND(CAST(p.cgi AS NUMERIC), 4) AS cgi,
            p.rank_pos AS rank_position,
            ROUND(CAST(p.percentile_pct AS NUMERIC), 2) AS percentile_pct,
            CASE
                WHEN p.rank_pos = 1 OR p.percentile_pct <= 15.0 THEN 'OURO'
                WHEN p.percentile_pct <= 35.0 THEN 'PRATA'
                WHEN p.percentile_pct <= 65.0 THEN 'BRONZE'
                WHEN p.percentile_pct <= 85.0 THEN 'COBRE'
                ELSE 'FERRO'
            END AS medal
        FROM farm_percentile p
        ORDER BY p.id_enterprise ASC, p.rank_pos ASC
    """, nativeQuery = true)
    List<com.ourosapp.springapi.repository.projection.FarmRankingProjection> calculateFarmRankingsByEnterprise(
            @org.springframework.data.repository.query.Param("idEnterprise") Long idEnterprise
    );

    /**
     * Calcula o ranqueamento, indicadores (CAA, CEA, CGI), percentil e medalhas para todas as fazendas de todas as integradoras.
     *
     * @return lista de projeções ordenadas por integradora e classificação
     */
    @org.springframework.data.jpa.repository.Query(value = """
        WITH water_agg AS (
            SELECT id_farm, COALESCE(SUM(end_hydrometer - start_hydrometer), 0) AS total_water
            FROM water_registries
            GROUP BY id_farm
        ),
        energy_agg AS (
            SELECT id_farm, COALESCE(SUM(energy_consumption), 0) AS total_energy
            FROM energy_registries
            GROUP BY id_farm
        ),
        farm_base AS (
            SELECT
                f.id AS farm_id,
                f.name AS farm_name,
                f.region AS region,
                NULL::INTEGER AS poultry_capacity,
                f.chickens_now AS chickens_now,
                f.id_enterprise AS id_enterprise,
                CASE
                    WHEN COALESCE(f.chickens_now, 0) > 0 THEN (COALESCE(w.total_water, 0) * 1.0 / f.chickens_now)
                    ELSE 0.0
                END AS caa,
                CASE
                    WHEN COALESCE(f.chickens_now, 0) > 0 THEN (COALESCE(e.total_energy, 0) * 1.0 / f.chickens_now)
                    ELSE 0.0
                END AS cea
            FROM farms f
            LEFT JOIN water_agg w ON w.id_farm = f.id
            LEFT JOIN energy_agg e ON e.id_farm = f.id
        ),
        farm_cgi AS (
            SELECT
                b.*,
                (b.caa * 0.7 + b.cea * 0.3) AS cgi
            FROM farm_base b
        ),
        farm_ranked AS (
            SELECT
                c.*,
                ROW_NUMBER() OVER (PARTITION BY c.id_enterprise ORDER BY c.cgi ASC, c.farm_id ASC) AS rank_pos,
                COUNT(*) OVER (PARTITION BY c.id_enterprise) AS total_count
            FROM farm_cgi c
        ),
        farm_percentile AS (
            SELECT
                r.*,
                CASE
                    WHEN r.total_count = 1 THEN 0.0
                    ELSE (r.rank_pos * 100.0 / r.total_count)
                END AS percentile_pct
            FROM farm_ranked r
        )
        SELECT
            p.farm_id AS farm_id,
            p.farm_name AS farm_name,
            p.region AS region,
            p.poultry_capacity AS poultry_capacity,
            p.chickens_now AS chickens_now,
            p.id_enterprise AS id_enterprise,
            ROUND(CAST(p.caa AS NUMERIC), 4) AS caa,
            ROUND(CAST(p.cea AS NUMERIC), 4) AS cea,
            ROUND(CAST(p.cgi AS NUMERIC), 4) AS cgi,
            p.rank_pos AS rank_position,
            ROUND(CAST(p.percentile_pct AS NUMERIC), 2) AS percentile_pct,
            CASE
                WHEN p.rank_pos = 1 OR p.percentile_pct <= 15.0 THEN 'OURO'
                WHEN p.percentile_pct <= 35.0 THEN 'PRATA'
                WHEN p.percentile_pct <= 65.0 THEN 'BRONZE'
                WHEN p.percentile_pct <= 85.0 THEN 'COBRE'
                ELSE 'FERRO'
            END AS medal
        FROM farm_percentile p
        ORDER BY p.id_enterprise ASC, p.rank_pos ASC
    """, nativeQuery = true)
    List<com.ourosapp.springapi.repository.projection.FarmRankingProjection> calculateAllFarmRankings();

    /**
     * Método conveniente que direciona para a consulta por empresa ou global dependendo do ID informado.
     *
     * @param idEnterprise identificador opcional da empresa
     * @return lista de projeções calculadas
     */
    default List<com.ourosapp.springapi.repository.projection.FarmRankingProjection> calculateFarmRankings(Long idEnterprise) {
        if (idEnterprise != null) {
            return calculateFarmRankingsByEnterprise(idEnterprise);
        }
        return calculateAllFarmRankings();
    }
}
