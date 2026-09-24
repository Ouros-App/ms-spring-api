package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.ChickenLeft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link ChickenLeft}.
 */
@Repository
public interface ChickenLeftRepository extends JpaRepository<ChickenLeft, Long> {

    /**
     * Busca todos os registros de saída de aves vinculados a uma fazenda específica.
     *
     * @param idFarm identificador único da fazenda
     * @return lista de registros de saída de aves da fazenda
     */
    List<ChickenLeft> findAllByIdFarm(Long idFarm);

    /**
     * Busca todos os registros de saída de aves vinculados a uma lista de IDs de fazendas.
     *
     * @param farmIds lista de identificadores únicos de fazendas
     * @return lista de registros de saída de aves encontrados
     */
    List<ChickenLeft> findAllByIdFarmIn(List<Long> farmIds);

    /**
     * Verifica se existe algum registro de saída de aves vinculado à fazenda.
     *
     * @param idFarm identificador único da fazenda
     * @return {@code true} se existir registro, {@code false} caso contrário
     */
    boolean existsByIdFarm(Long idFarm);
}
