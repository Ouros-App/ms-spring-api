package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.FarmOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações de persistência e consulta da entidade {@link FarmOwner}.
 */
@Repository
public interface FarmOwnerRepository extends JpaRepository<FarmOwner, Long> {

    /**
     * Busca um produtor rural pelo seu e-mail cadastrado.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return Optional contendo o produtor rural se encontrado, ou vazio caso contrário
     */
    Optional<FarmOwner> findByEmail(String email);

    /**
     * Busca um produtor rural pelo seu e-mail cadastrado, ignorando maiúsculas e minúsculas.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return Optional contendo o produtor rural se encontrado, ou vazio caso contrário
     */
    Optional<FarmOwner> findByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe um produtor rural cadastrado com o e-mail informado.
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir um produtor com esse e-mail, {@code false} caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se já existe um produtor rural cadastrado com o e-mail informado (ignorando maiúsculas e minúsculas).
     *
     * @param email endereço de e-mail a ser verificado
     * @return {@code true} se existir um produtor com esse e-mail, {@code false} caso contrário
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe um produtor rural cadastrado com o número de documento (CPF) informado.
     *
     * @param documentNumber número de CPF (apenas dígitos)
     * @return {@code true} se existir um produtor com esse documento, {@code false} caso contrário
     */
    boolean existsByDocumentNumber(String documentNumber);

    /**
     * Busca um produtor rural pelo seu número de documento (CPF).
     *
     * @param documentNumber número de CPF a ser pesquisado
     * @return Optional contendo o produtor rural se encontrado, ou vazio caso contrário
     */
    Optional<FarmOwner> findByDocumentNumber(String documentNumber);

    /**
     * Busca todos os produtores rurais vinculados a uma determinada fazenda.
     *
     * @param idFarm identificador único da fazenda
     * @return lista de produtores rurais associados à fazenda
     */
    List<FarmOwner> findAllByIdFarm(Long idFarm);

    /**
     * Busca todos os produtores rurais vinculados a qualquer uma das fazendas da lista informada.
     *
     * @param farmIds lista de identificadores das fazendas
     * @return lista de produtores rurais associados
     */
    List<FarmOwner> findAllByIdFarmIn(List<Long> farmIds);
}