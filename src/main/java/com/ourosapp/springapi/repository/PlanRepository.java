package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link Plan}.
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * Verifica se já existe um plano com o título informado, ignorando maiúsculas e minúsculas.
     *
     * @param title título do plano
     * @return {@code true} se existir, {@code false} caso contrário
     */
    boolean existsByTitleIgnoreCase(String title);

    /**
     * Busca um plano pelo título, ignorando maiúsculas e minúsculas.
     *
     * @param title título do plano
     * @return Optional contendo o plano se encontrado
     */
    Optional<Plan> findByTitleIgnoreCase(String title);
}
