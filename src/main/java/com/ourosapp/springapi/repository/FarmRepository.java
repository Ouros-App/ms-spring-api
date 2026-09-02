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
}
