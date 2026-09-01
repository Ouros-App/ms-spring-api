package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Adm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link Adm}.
 */
@Repository
public interface AdmRepository extends JpaRepository<Adm, Long> {

    /**
     * Busca um administrador pelo e-mail exato.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return {@link Optional} contendo o administrador se encontrado, ou vazio caso contrário
     */
    Optional<Adm> findByEmail(String email);

    /**
     * Busca um administrador pelo e-mail, ignorando diferenças entre maiúsculas e minúsculas.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return {@link Optional} contendo o administrador se encontrado, ou vazio caso contrário
     */
    Optional<Adm> findByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe um administrador cadastrado com o e-mail informado.
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir um administrador com este e-mail, {@code false} caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se já existe um administrador cadastrado com o e-mail informado (case-insensitive).
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir um administrador com este e-mail, {@code false} caso contrário
     */
    boolean existsByEmailIgnoreCase(String email);
}
