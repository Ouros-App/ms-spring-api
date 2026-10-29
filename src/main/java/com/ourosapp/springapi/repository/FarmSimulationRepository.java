package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.FarmSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para operações de persistência de {@link FarmSimulation}.
 */
@Repository
public interface FarmSimulationRepository extends JpaRepository<FarmSimulation, Long> {

    /**
     * Lista os cenários de simulação pertencentes a uma empresa integradora, ordenados pelo ID decrescente.
     *
     * @param idEnterprise identificador da empresa integradora
     * @return lista de simulações da empresa
     */
    List<FarmSimulation> findByIdEnterpriseOrderByIdDesc(Long idEnterprise);

    /**
     * Lista os cenários de simulação pertencentes a uma fazenda específica, ordenados pelo ID decrescente.
     *
     * @param idFarm identificador da fazenda
     * @return lista de simulações da fazenda
     */
    List<FarmSimulation> findByIdFarmOrderByIdDesc(Long idFarm);

    /**
     * Lista os cenários de simulação de uma fazenda e empresa integradora específicas.
     *
     * @param idEnterprise identificador da empresa integradora
     * @param idFarm       identificador da fazenda
     * @return lista de simulações filtradas
     */
    List<FarmSimulation> findByIdEnterpriseAndIdFarmOrderByIdDesc(Long idEnterprise, Long idFarm);
}
