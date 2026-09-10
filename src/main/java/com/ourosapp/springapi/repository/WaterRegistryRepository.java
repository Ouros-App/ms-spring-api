package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.WaterRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link WaterRegistry}.
 */
@Repository
public interface WaterRegistryRepository extends JpaRepository<WaterRegistry, Long> {

    /**
     * Busca todos os registros de medição de água vinculados a uma fazenda específica.
     *
     * @param idFarm identificador único da fazenda
     * @return lista de registros de medição de água da fazenda
     */
    List<WaterRegistry> findAllByIdFarm(Long idFarm);

    /**
     * Busca todos os registros de medição de água vinculados a uma lista de IDs de fazendas.
     *
     * @param farmIds lista de identificadores únicos de fazendas
     * @return lista de registros de medição de água encontrados
     */
    List<WaterRegistry> findAllByIdFarmIn(List<Long> farmIds);

    /**
     * Verifica se existe algum registro de medição de água vinculado à fazenda.
     *
     * @param idFarm identificador único da fazenda
     * @return {@code true} se existir registro, {@code false} caso contrário
     */
    boolean existsByIdFarm(Long idFarm);
}
