package com.ourosapp.springapi.repository;

import com.ourosapp.springapi.entity.CompanyEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyEmployeeRepository extends JpaRepository<CompanyEmployee, Long> {
    Optional<CompanyEmployee> findByEmail(String email);
    boolean existsByEmail(String email);
}