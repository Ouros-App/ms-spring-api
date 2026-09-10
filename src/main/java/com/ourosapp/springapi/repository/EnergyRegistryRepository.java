package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.EnergyRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para gerenciamento e operações de persistência da entidade {@link EnergyRegistry}.
 */
@Repository
public interface EnergyRegistryRepository extends JpaRepository<EnergyRegistry, Long> {

    /**
     * Busca todos os registros de energia vinculados a uma fazenda específica.
     *
     * @param idFarm ID da fazenda
     * @return Lista de registros de energia encontrados
     */
    List<EnergyRegistry> findByIdFarm(Long idFarm);

    /**
     * Busca todos os registros de energia vinculados a uma lista de IDs de fazendas.
     *
     * @param idFarms Lista de IDs de fazendas
     * @return Lista de registros de energia encontrados
     */
    List<EnergyRegistry> findByIdFarmIn(List<Long> idFarms);
}
