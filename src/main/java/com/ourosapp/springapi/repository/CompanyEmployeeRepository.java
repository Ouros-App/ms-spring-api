package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.CompanyEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link CompanyEmployee}.
 */
@Repository
public interface CompanyEmployeeRepository extends JpaRepository<CompanyEmployee, Long> {

    /**
     * Busca um funcionário pelo e-mail corporativo exato.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return {@link Optional} contendo o funcionário se encontrado, ou vazio caso contrário
     */
    Optional<CompanyEmployee> findByEmail(String email);

    /**
     * Busca um funcionário pelo e-mail corporativo, ignorando diferenças entre maiúsculas e minúsculas.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return {@link Optional} contendo o funcionário se encontrado, ou vazio caso contrário
     */
    Optional<CompanyEmployee> findByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe um funcionário cadastrado com o e-mail corporativo informado.
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir um funcionário com este e-mail, {@code false} caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se já existe um funcionário cadastrado com o e-mail informado (case-insensitive).
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir um funcionário com este e-mail, {@code false} caso contrário
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Busca um funcionário pelo número do documento (CPF).
     *
     * @param documentNumber número do documento / CPF a ser pesquisado
     * @return {@link Optional} contendo o funcionário se encontrado, ou vazio caso contrário
     */
    Optional<CompanyEmployee> findByDocumentNumber(String documentNumber);

    /**
     * Verifica se já existe um funcionário cadastrado com o número do documento (CPF) informado.
     *
     * @param documentNumber número do documento / CPF a ser verificado
     * @return {@code true} se existir um funcionário com este documento, {@code false} caso contrário
     */
    boolean existsByDocumentNumber(String documentNumber);
}