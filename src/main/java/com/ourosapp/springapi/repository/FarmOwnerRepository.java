package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.FarmOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link FarmOwner}.
 */
@Repository
public interface FarmOwnerRepository extends JpaRepository<FarmOwner, Long> {

    /**
     * Busca um produtor rural pelo e-mail exato.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return {@link Optional} contendo o produtor rural se encontrado, ou vazio caso contrário
     */
    Optional<FarmOwner> findByEmail(String email);

    /**
     * Busca um produtor rural pelo e-mail, ignorando diferenças entre maiúsculas e minúsculas.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return {@link Optional} contendo o produtor rural se encontrado, ou vazio caso contrário
     */
    Optional<FarmOwner> findByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe um produtor rural cadastrado com o e-mail informado.
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir um produtor rural com este e-mail, {@code false} caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se já existe um produtor rural cadastrado com o e-mail informado (case-insensitive).
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir um produtor rural com este e-mail, {@code false} caso contrário
     */
    boolean existsByEmailIgnoreCase(String email);
}