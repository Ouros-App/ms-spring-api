package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
