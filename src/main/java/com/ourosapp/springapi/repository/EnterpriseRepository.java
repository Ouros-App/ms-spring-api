package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Enterprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link Enterprise}.
 */
@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long> {

    /**
     * Verifica se já existe uma empresa cadastrada com o número de documento (CNPJ) informado.
     *
     * @param documentNumber número do documento / CNPJ
     * @return {@code true} se existir, {@code false} caso contrário
     */
    boolean existsByDocumentNumber(String documentNumber);

    /**
     * Verifica se já existe uma empresa cadastrada com o e-mail informado (ignorando maiúsculas e minúsculas).
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir, {@code false} caso contrário
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Busca uma empresa pelo número de documento (CNPJ).
     *
     * @param documentNumber número do documento
     * @return Optional contendo a empresa se encontrada
     */
    Optional<Enterprise> findByDocumentNumber(String documentNumber);

    /**
     * Busca uma empresa pelo e-mail corporativo.
     *
     * @param email endereço de e-mail
     * @return Optional contendo a empresa se encontrada
     */
    Optional<Enterprise> findByEmailIgnoreCase(String email);
}

