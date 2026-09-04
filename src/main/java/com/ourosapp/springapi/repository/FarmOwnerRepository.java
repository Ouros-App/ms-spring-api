package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.FarmOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório de dados para a entidade FarmOwner.
 */
@Repository
public interface FarmOwnerRepository extends JpaRepository<FarmOwner, Long> {
    Optional<FarmOwner> findByEmail(String email);
    Optional<FarmOwner> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
}