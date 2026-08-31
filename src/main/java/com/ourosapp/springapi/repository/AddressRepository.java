package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para operações de persistência da entidade {@link Address}.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
}
