package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.CompanyEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyEmployeeRepository extends JpaRepository<CompanyEmployee, Long> {
    Optional<CompanyEmployee> findByEmail(String email);
    Optional<CompanyEmployee> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<CompanyEmployee> findByDocumentNumber(String documentNumber);
    boolean existsByDocumentNumber(String documentNumber);
}